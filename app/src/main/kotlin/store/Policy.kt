package store

/**
 * 편의점 시스템의 정책 관련 클래스들
 * - Thresholds : 재고 부족 기준, 유통기한 임박 기준 등
 * - DiscountPolicy : 유통기한별 할인율 정책
 */
data class Thresholds(
    val lowStockRatio: Double = 0.30,   // 재고 비율 30% 이하면 부족
    val expireSoonDays: Int = 3         // D-3 이내면 유통기한 임박
)

/**
 * 유통기한에 따른 할인율 정의
 * 예: D-1 → 40%, D-3 → 20%, D-7 → 10%
 */
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
