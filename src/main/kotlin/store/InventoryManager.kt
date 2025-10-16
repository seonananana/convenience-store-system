package store

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class InventoryManager(
    private val products: List<Product>,
    private val thresholds: Thresholds = Thresholds(),
    private val discountPolicy: DiscountPolicy = DiscountPolicy(),
    private val subjective: SubjectiveWeights = SubjectiveWeights()
) {
    private val byName = products.associateBy { it.name }

    /** 자동 발주 필요 품목 산출 (재고율 < lowStockRatio) */
    private fun buildAutoOrders(): List<PurchaseOrder> =
        products.mapNotNull { p ->
            val rate = if (p.idealStock == 0) 1.0 else p.currentStock.toDouble() / p.idealStock
            if (rate < thresholds.lowStockRatio) PurchaseOrder(p.name, (p.idealStock - p.currentStock).coerceAtLeast(0))
            else null
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
            .sortedBy { it.second }

    /** 재고 자산가치 = Σ(현재재고 × 가격) */
    private fun totalInventoryValue(): Long =
        products.sumOf { it.currentStock.toLong() * it.price }

    /**
     * 판매 집계 (qtyByName), 총매출, TOP5 (상품, 개수)
     * - TOP5는 ‘모든 상품’을 대상으로 매출액 기준 정렬하여 항상 5개를 보장(판매 0도 표시)
     */
    private fun aggregateSales(sales: List<Sale>): Pair<Long, List<Pair<Product, Int>>> {
        val soldQty = sales.groupBy { it.productName }.mapValues { it.value.sumOf { s -> s.qty } }

        val totalRevenue = products.sumOf { p ->
            val qty = soldQty[p.name] ?: 0
            qty.toLong() * p.price
        }

        val sorted = products
            .map { p -> p to (soldQty[p.name] ?: 0) }
            .sortedByDescending { (p, qty) -> p.price * qty }

        // 항상 5개 보장 (상품이 5개 미만이면 0매출 filler를 붙임)
        val top = if (sorted.size >= 5) {
            sorted.take(5)
        } else {
            val fillers = List(5 - sorted.size) { Pair(Product("—", ProductCategory.SNACK, 0, 0, 0), 0) }
            (sorted + fillers).take(5)
        }

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

    /** 종합 운영 현황 행 만들기 (재고율·판매·매출·회전율·임박·발주) */
    private fun buildOperationsOverview(sales: List<Sale>, today: LocalDate): List<OperationRow> {
        val qtyByName = sales.groupBy { it.productName }.mapValues { it.value.sumOf { s -> s.qty } }
        return products.map { p ->
            val salesQty = qtyByName[p.name] ?: 0
            val revenue = salesQty * p.price
            val stockRatePct =
                if (p.idealStock <= 0) 100.0
                else (p.currentStock.toDouble() / p.idealStock) * 100.0
            val turnoverPct =
                if (p.idealStock <= 0) 0.0
                else (salesQty.toDouble() / p.idealStock) * 100.0
            val expDays = p.expiryDate?.let { ChronoUnit.DAYS.between(today, it).toInt() }
            val reorderNeed = run {
                val rate = if (p.idealStock == 0) 1.0 else p.currentStock.toDouble() / p.idealStock
                if (rate < thresholds.lowStockRatio) (p.idealStock - p.currentStock).coerceAtLeast(0) else null
            }
            OperationRow(
                name = p.name,
                category = p.category,
                price = p.price,
                current = p.currentStock,
                ideal = p.idealStock,
                stockRatePct = stockRatePct,
                salesQty = salesQty,
                revenue = revenue,
                turnoverPct = turnoverPct,
                expiringDays = expDays,
                reorderNeed = reorderNeed
            )
        }.sortedWith(
            compareByDescending<OperationRow> { it.revenue }
                .thenBy { it.stockRatePct } // 매출 같으면 재고율 낮은 순을 위로
        )
    }

    /** 외부 노출: 리포트 생성 (주관 레이어 포함) */
    fun report(sales: List<Sale>, today: LocalDate = LocalDate.now()): Report {
        val qtyByName = sales.groupBy { it.productName }.mapValues { it.value.sumOf { s -> s.qty } }

        val lowStock = buildAutoOrders()
        val expiring = buildExpiring(today)
        val invValue = totalInventoryValue()
        val salesAgg = aggregateSales(sales)
        val lowestTurnover = lowestTurnover(sales)

        // 주관 발주/할인 제안
        val reorderStrategy = ReorderStrategy(subjective, thresholds.lowStockRatio)
        val reorderSubjective = reorderStrategy.suggest(products, qtyByName)

        val pricingStrategy = PricingStrategy(subjective, discountPolicy)
        val pricingSubjective = products.map { p ->
            val d = p.expiryDate?.let { ChronoUnit.DAYS.between(today, it).toInt() }
            val (rate, discounted) = pricingStrategy.suggestDiscount(p, qtyByName[p.name] ?: 0, d)
            Triple(p, rate, discounted)
        }

        val operations = buildOperationsOverview(sales, today)

        return Report.build(
            today = today,
            lowStock = lowStock,
            expiring = expiring,
            totalInventoryValue = invValue,
            sales = salesAgg,
            lowestTurnover = lowestTurnover,
            reorderSubjective = reorderSubjective,
            pricingSubjective = pricingSubjective,
            operations = operations
        )
    }
}
