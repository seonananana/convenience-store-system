package store

/** 재고 부족/유통기한 임박 기준 */
data class Thresholds(
    val lowStockRatio: Double = 0.30,   // 재고율 30% 미만이면 부족
    val expireSoonDays: Int = 3         // D-3 이내면 유통기한 임박
)

/** 유통기한 D-n 별 할인율 */
data class DiscountPolicy(
    val steps: List<Pair<Int, Double>> = listOf(
        1 to 0.40,
        3 to 0.20,
        7 to 0.10
    )
) {
    fun rateForDays(daysToExpire: Int): Double =
        steps.firstOrNull { daysToExpire <= it.first }?.second ?: 0.0
}
