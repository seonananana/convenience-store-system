//상품의 원천 데이터와 기본 계산
package store
import java.time.LocalDate

data class Product(
    val name: String,
    val category: ProductCategory,
    val price: Int,            // 원
    val targetStock: Int,      // 적정 재고
    var stock: Int,            // 현재 재고
    val expiresAt: LocalDate? = null
) {
    fun stockRatio(): Double =
        if (targetStock <= 0) 0.0 else stock.toDouble() / targetStock

    fun stockStatus(threshold: Double = 0.30): String = when {
        stock <= 0 -> "품절"
        stockRatio() <= threshold -> "부족"
        else -> "충분"
    }
}
