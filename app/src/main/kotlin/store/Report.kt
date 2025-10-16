//출력 포맷(문자열) 생성. 콘솔/파일 등 어디든 재사용
package store
import java.time.LocalDate

data class Report(
    val date: LocalDate,
    val lowStock: List<PurchaseOrder>,
    val expiring: List<Triple<Product, Int, Int>>, // (상품, D-?, 할인가)
    val totalInventoryValue: Long,
    val totalRevenue: Long,
    val topSellers: List<Pair<Product, Int>>
) {
    companion object {
        fun build(
            today: LocalDate,
            lowStock: List<PurchaseOrder>,
            expiring: List<Triple<Product, Int, Int>>,
            totalInventoryValue: Long,
            sales: Pair<Long, List<Pair<Product, Int>>>
        ) = Report(
            date = today,
            lowStock = lowStock,
            expiring = expiring,
            totalInventoryValue = totalInventoryValue,
            totalRevenue = sales.first,
            topSellers = sales.second
        )
    }

    fun render(): String = buildString {
        appendLine("=== 편의점 리포트 (${date}) ===")
        appendLine("\n[1] 긴급 재고(발주 제안)")
        if (lowStock.isEmpty()) appendLine(" - 없음")
        else lowStock.forEach { appendLine(" - ${it.productName}: +${it.qty}개") }

        appendLine("\n[2] 유통기한 임박/할인")
        if (expiring.isEmpty()) appendLine(" - 없음")
        else expiring.forEach { (p, d, discounted) ->
            appendLine(" - ${p.name} (D-${d}) → ${p.price}원 → ${discounted}원")
        }

        appendLine("\n[3] 재고 자산가치: ${"%,d".format(totalInventoryValue)}원")
        appendLine("[4] 금일 매출: ${"%,d".format(totalRevenue)}원")

        appendLine("\n[5] 베스트셀러 TOP5")
        if (topSellers.isEmpty()) appendLine(" - 없음")
        else topSellers.forEachIndexed { i, (p, qty) ->
            appendLine(" ${i + 1}. ${p.name} (${qty}개)")
        }
    }
}
