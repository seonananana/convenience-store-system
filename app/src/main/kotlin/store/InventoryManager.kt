//한 번에 분석/집계/발주 추천/매출 집계 수행
//로직(핵심API)
package store

import java.time.LocalDate

class InventoryManager(
    private val products: MutableList<Product>,
    private val thresholds: Thresholds = Thresholds(),
    private val discountPolicy: DiscountPolicy = DiscountPolicy()
) {
    fun lowStockAlerts(): List<PurchaseOrder> =
        products
            .filter { it.stockStatus(thresholds.lowStockRatio) != "충분" }
            .map { p ->
                val needed = (p.targetStock - p.stock).coerceAtLeast(0)
                PurchaseOrder(p.name, needed)
            }
            .filter { it.qty > 0 }

    fun expiringSoon(today: LocalDate = LocalDate.now()): List<Triple<Product, Int, Int>> =
        products.expiringWithin(thresholds.expireSoonDays, today)
            .map { p ->
                val d = p.daysToExpire(today)!!
                Triple(p, d, p.discountedPrice(discountPolicy, today))
            }

    fun salesSummary(sales: List<Sale>): Pair<Long, List<Pair<Product, Int>>> {
        val salesMap = sales.groupBy { it.productName }.mapValues { it.value.sumOf { s -> s.qty } }
        val totalRevenue = products.sumOf { p -> (salesMap[p.name] ?: 0) * p.price }.toLong()
        val top = products.topSellers(salesMap, k = 5)
        return totalRevenue to top
    }

    fun report(sales: List<Sale>, today: LocalDate = LocalDate.now()): Report =
        Report.build(
            today = today,
            lowStock = lowStockAlerts(),
            expiring = expiringSoon(today),
            totalInventoryValue = products.totalInventoryValue(),
            sales = salesSummary(sales)
        )
}
