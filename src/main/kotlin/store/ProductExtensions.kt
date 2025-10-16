package store

import java.time.LocalDate

/** N일 내 유통기한 임박 필터 */
fun List<Product>.expiringWithin(days: Long, today: LocalDate = LocalDate.now()): List<Product> =
    this.filter { it.expiryDate != null && it.daysToExpireFromToday(today) in 0..days }

/** 매출 TOP1 (이름→수량, 상품맵) → (상품명, 매출액) */
fun Map<String, Int>.bestSellerByRevenue(productsByName: Map<String, Product>): Pair<String, Int>? =
    this.maxByOrNull { (name, qty) -> qty * (productsByName[name]?.price ?: 0) }
        ?.let { (name, qty) -> name to qty * (productsByName[name]!!.price) }

/** 재고 회전율(%) = (판매수량 / 적정재고) * 100 */
fun Product.turnoverPercent(periodSalesQty: Int): Double =
    if (idealStock <= 0) 0.0 else (periodSalesQty.toDouble() / idealStock) * 100.0

/** 자동 발주 필요 여부 (재고율 < threshold) → 필요수량 */
fun Product.reorderNeeded(threshold: Double): Int? {
    val rate = if (idealStock == 0) 1.0 else currentStock.toDouble() / idealStock
    return if (rate < threshold) (idealStock - currentStock).coerceAtLeast(0) else null
}
