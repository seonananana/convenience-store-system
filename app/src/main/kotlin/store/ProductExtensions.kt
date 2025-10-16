//기존 모델 변경 없이 기능 확장(week3개념)
//(확장함수모음)
// 날짜/유통기한
package store
import java.time.LocalDate
import java.time.Period

fun Product.daysToExpire(today: LocalDate = LocalDate.now()): Int? =
    expiresAt?.let { Period.between(today, it).days }

// 할인정책 적용
fun Product.discountRate(policy: DiscountPolicy, today: LocalDate = LocalDate.now()): Double {
    val d = daysToExpire(today) ?: return 0.0
    return policy.rateForDays(d)
}
fun Product.discountedPrice(policy: DiscountPolicy, today: LocalDate = LocalDate.now()): Int =
    (price * (1.0 - discountRate(policy, today))).toInt()

// 컬렉션 확장
fun List<Product>.lowStock(threshold: Double = 0.30): List<Product> =
    filter { it.stockStatus(threshold) != "충분" }

fun List<Product>.expiringWithin(days: Int, today: LocalDate = LocalDate.now()): List<Product> =
    filter { p -> p.expiresAt?.let { !it.isBefore(today) && !it.isAfter(today.plusDays(days.toLong())) } ?: false }

fun List<Product>.totalInventoryValue(): Long =
    sumOf { it.stock.toLong() * it.price }

fun List<Product>.topSellers(sales: Map<String, Int>, k: Int = 5): List<Pair<Product, Int>> =
    map { p -> p to (sales[p.name] ?: 0) }
        .sortedByDescending { it.second }
        .take(k)
