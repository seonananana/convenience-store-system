package store

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Product(
    val name: String,
    val category: ProductCategory,
    val price: Int,
    var currentStock: Int,
    val idealStock: Int,
    val expiryDate: LocalDate? = null,
    /** 음료: 용량(ml) — CSV 컬럼이 없으면 이름(예: "500ml", "1.5L")에서 추정 */
    val volumeMl: Int? = null,
    /** 생활용품: 브랜드명 — CSV에 없으면 null */
    val brand: String? = null
) {
    /** 오늘 기준 남은 유통기한(일). null 이면 매우 큰 값으로 간주 */
    fun daysToExpireFromToday(today: LocalDate = LocalDate.now()): Long =
        expiryDate?.let { ChronoUnit.DAYS.between(today, it) } ?: Long.MAX_VALUE
}
