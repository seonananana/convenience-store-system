package store

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class InventoryManager(
    private val products: List<Product>,
    private val thresholds: Thresholds = Thresholds(),
    private val discountPolicy: DiscountPolicy = DiscountPolicy()
) {
    private val byName = products.associateBy { it.name }

    /** 자동 발주 필요 품목 산출 (재고율 < lowStockRatio) */
    private fun buildAutoOrders(): List<PurchaseOrder> =
        products.mapNotNull { p ->
            val rate = if (p.idealStock == 0) 1.0 else p.currentStock.toDouble() / p.idealStock
            if (rate < thresholds.lowStockRatio) {
                PurchaseOrder(p.name, (p.idealStock - p.currentStock).coerceAtLeast(0))
            } else null
        }

    /** 유통기한 임박 + 할인 적용 목록 (상품, D-?, 할인가) */
    private fun buildExpiring(today: LocalDate): List<Triple<Product, Int, Int>> =
        products
            .filter { it.expiryDate != null }
            .mapNotNull { p ->
                val d = ChronoUnit.DAYS.between(today, p.expiryDate).toInt()
                if (d in 0..thresholds.expireSoonDays) {
                    val discounted = (p.price * (1 - discountPolicy.rateForDays(d))).toInt()
                    Triple(p, d, discounted)
                } else null
            }
            .sortedBy { it.second } // D-? 오름차순

    /** 재고 자산가치 = Σ(현재재고 × 가격) */
    private fun totalInventoryValue(): Long =
        products.sumOf { it.currentStock.toLong() * it.price }

    /** 판매 집계 (qtyByName), 총매출, TOP5 (상품, 개수) */
    private fun aggregateSales(sales: List<Sale>): Pair<Long, List<Pair<Product, Int>>> {
        val qtyByName = sales.groupBy { it.productName }.mapValues { it.value.sumOf { s -> s.qty } }
        val totalRevenue = qtyByName.entries.sumOf { (name, qty) ->
            val price = byName[name]?.price ?: 0
            qty.toLong() * price
        }
        val top = qtyByName.entries
            .sortedByDescending { (name, qty) -> (byName[name]?.price ?: 0) * qty }
            .take(5)
            .mapNotNull { (name, qty) -> byName[name]?.let { it to qty } }
        return totalRevenue to top
    }

    /** 재고 회전율 최저 = (판매수량 / 적정재고)*100 이 가장 낮은 상품 */
    private fun lowestTurnover(sales: List<Sale>): Pair<Product, Double>? {
        val qtyByName = sales.groupBy { it.productName }.mapValues { it.value.sumOf { s -> s.qty } }
        return products.minByOrNull { p ->
            if (p.idealStock <= 0) Double.POSITIVE_INFINITY
            else (qtyByName[p.name]?.toDouble() ?: 0.0) / p.idealStock
        }?.let { p ->
            val rate = if (p.idealStock <= 0) 0.0 else (qtyByName[p.name]?.toDouble() ?: 0.0) / p.idealStock * 100.0
            p to rate
        }
    }

    /** 외부 노출: 리포트 한 번에 생성 */
    fun report(sales: List<Sale>, today: LocalDate = LocalDate.now()): Report {
        val lowStock = buildAutoOrders()
        val expiring = buildExpiring(today)
        val invValue = totalInventoryValue()
        val salesAgg = aggregateSales(sales)
        val lowestTurnover = lowestTurnover(sales)
        return Report.build(
            today = today,
            lowStock = lowStock,
            expiring = expiring,
            totalInventoryValue = invValue,
            sales = salesAgg,
            lowestTurnover = lowestTurnover
        )
    }
}
