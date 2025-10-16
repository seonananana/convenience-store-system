package store

import kotlin.math.abs
import kotlin.math.max

/** 운영자 주관 가중치 (합계는 자동 정규화) */
data class SubjectiveWeights(
    val weightTurnover: Double = 0.30,     // 회전율 선호
    val weightMargin: Double = 0.30,       // 마진(가격으로 근사) 선호
    val weightStability: Double = 0.20,    // 재고 안정
    val weightFreshPriority: Double = 0.20 // 신선식품 가점
)

data class ProductMetric(
    val turnover: Double,     // 0~1 (판매/적정재고, cap → 1.5)
    val marginApprox: Double, // 0~1 (가격/10000 근사 정규화)
    val stability: Double,    // 0~1 (적정=1과의 근접도)
    val freshBoost: Double    // 0 또는 1 (FOOD=1)
)

object MetricBuilder {
    fun forProduct(p: Product, periodSalesQty: Int): ProductMetric {
        val turnover =
            if (p.idealStock <= 0) 0.0
            else (periodSalesQty.toDouble() / p.idealStock).coerceIn(0.0, 1.5) / 1.5

        val marginApprox = (p.price.toDouble() / 10_000.0).coerceIn(0.0, 1.0)

        val ratio = if (p.idealStock <= 0) 1.0 else (p.currentStock.toDouble() / p.idealStock)
        val stability = (1.0 - abs(1.0 - ratio)).coerceIn(0.0, 1.0)

        val freshBoost = if (p.category == ProductCategory.FOOD) 1.0 else 0.0

        return ProductMetric(turnover, marginApprox, stability, freshBoost)
    }
}

object Scorer {
    fun score(metric: ProductMetric, w: SubjectiveWeights): Double {
        val totalW = max(1e-9, w.weightTurnover + w.weightMargin + w.weightStability + w.weightFreshPriority)
        return (
                metric.turnover       * w.weightTurnover +
                        metric.marginApprox   * w.weightMargin +
                        metric.stability      * w.weightStability +
                        metric.freshBoost     * w.weightFreshPriority
                ) / totalW
    }
}

/** 주관 발주 우선순위 전략 */
class ReorderStrategy(private val weights: SubjectiveWeights, private val lowStockRatio: Double) {
    fun suggest(products: List<Product>, salesQtyByName: Map<String, Int>): List<Triple<Product, Int, Double>> {
        val scored = products.mapNotNull { p ->
            val need = reorderNeed(p, lowStockRatio) ?: return@mapNotNull null
            val metrics = MetricBuilder.forProduct(p, salesQtyByName[p.name] ?: 0)
            val score = Scorer.score(metrics, weights)
            Triple(p, need, score)
        }
        return scored.sortedByDescending { it.third }
    }

    private fun reorderNeed(p: Product, threshold: Double): Int? {
        val rate = if (p.idealStock == 0) 1.0 else p.currentStock.toDouble() / p.idealStock
        return if (rate < threshold) (p.idealStock - p.currentStock).coerceAtLeast(0) else null
    }
}

/** 주관 동적 할인 전략 */
class PricingStrategy(
    private val weights: SubjectiveWeights,
    private val baseDiscountPolicy: DiscountPolicy
) {
    /** @return (제안 할인율 0.0~0.5, 적용가) */
    fun suggestDiscount(p: Product, todaySalesQty: Int, daysToExpire: Int?): Pair<Double, Int> {
        val expiryRate = daysToExpire?.let { baseDiscountPolicy.rateForDays(it) } ?: 0.0

        val metrics = MetricBuilder.forProduct(p, todaySalesQty)
        val baseScore = Scorer.score(metrics, weights) // 높을수록 “잘 팔리고 안정적”
        val scarcity = if (p.idealStock == 0) 1.0 else (p.currentStock.toDouble() / p.idealStock).coerceIn(0.0, 2.0)
        val overstockPressure = (scarcity - 1.0).coerceAtLeast(0.0) // 1 초과만 과잉재고

        // 점수가 낮고 과잉재고일수록 가산 할인(최대 +0.40p 근사)
        val behavioralAdj = (1.0 - baseScore) * 0.25 + overstockPressure * 0.15
        val finalRate = (expiryRate + behavioralAdj).coerceIn(0.0, 0.50)

        val discounted = (p.price * (1 - finalRate)).toInt()
        return finalRate to discounted
    }
}
