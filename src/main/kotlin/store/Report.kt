package store

import java.time.LocalDate
import java.util.Locale

data class Report(
    val date: LocalDate,
    val lowStock: List<PurchaseOrder>,
    val expiring: List<Triple<Product, Int, Int>>, // (상품, D-?, 할인가)
    val totalInventoryValue: Long,
    val totalRevenue: Long,
    val topSellers: List<Pair<Product, Int>>,
    val lowestTurnover: Pair<Product, Double>?,
    // 주관 점수 기반 제안
    val reorderSubjective: List<Triple<Product, Int, Double>>,
    val pricingSubjective: List<Triple<Product, Double, Int>>,
    // 종합 운영 현황
    val operations: List<OperationRow>,
    // 신규: 카테고리 세부 분류
    val drilldown: Drilldown
) {
    companion object {
        fun build(
            today: LocalDate,
            lowStock: List<PurchaseOrder>,
            expiring: List<Triple<Product, Int, Int>>,
            totalInventoryValue: Long,
            sales: Pair<Long, List<Pair<Product, Int>>>,
            lowestTurnover: Pair<Product, Double>?,
            reorderSubjective: List<Triple<Product, Int, Double>>,
            pricingSubjective: List<Triple<Product, Double, Int>>,
            operations: List<OperationRow>,
            drilldown: Drilldown
        ) = Report(
            date = today,
            lowStock = lowStock,
            expiring = expiring,
            totalInventoryValue = totalInventoryValue,
            totalRevenue = sales.first,
            topSellers = sales.second,
            lowestTurnover = lowestTurnover,
            reorderSubjective = reorderSubjective,
            pricingSubjective = pricingSubjective,
            operations = operations,
            drilldown = drilldown
        )
    }

    // ---------- formatting helpers ----------
    private fun fmtMoney(v: Number): String =
        String.format(Locale.getDefault(), "%,d", v.toLong())

    private fun fmtPct1(v: Number): String =
        String.format(Locale.getDefault(), "%.1f%%", v.toDouble())

    private fun pad(s: String, w: Int): String =
        if (s.length >= w) s.take(w) else s + " ".repeat(w - s.length)

    private fun groupSummaryLine(title: String, keyLabel: String, items: List<Product>): String {
        val kinds = items.size
        val totalStock = items.sumOf { it.currentStock }
        return " - $title $keyLabel: ${kinds}종 (재고합계 ${fmtMoney(totalStock)}개)"
    }

    // ---------- renderer ----------
    fun render(): String = buildString {
        appendLine("=== 편의점 리포트 ($date) ===")

        // [1] 자동 발주
        appendLine()
        appendLine("[1] 🚨 자동 발주 필요 품목")
        if (lowStock.isEmpty()) {
            appendLine(" - 없음")
        } else {
            lowStock.forEach { po ->
                appendLine(" - ${po.productName}: +${po.qty}개")
            }
        }

        // [2] 유통기한 임박/할인
        appendLine()
        appendLine("[2] ⚠ 유통기한 임박/할인")
        if (expiring.isEmpty()) {
            appendLine(" - 없음")
        } else {
            expiring.forEach { (p, d, discounted) ->
                appendLine(" - ${p.name} (D-$d) → ${fmtMoney(p.price)}원 → ${fmtMoney(discounted)}원")
            }
        }

        // [3][4] 자산/매출
        appendLine()
        appendLine("[3] 💰 재고 자산가치: ${fmtMoney(totalInventoryValue)}원")
        appendLine("[4] 📈 금일/기간 매출: ${fmtMoney(totalRevenue)}원")

        // [5] 베스트셀러 TOP5
        appendLine()
        appendLine("[5] 🏆 베스트셀러 TOP5 (매출액 기준)")
        if (topSellers.isEmpty()) {
            appendLine(" - 없음")
        } else {
            topSellers.forEachIndexed { i, (p, qty) ->
                if (p.price == 0 && p.name == "—") {
                    appendLine(" ${i + 1}. (빈 슬롯)")
                } else {
                    val revenue = qty * p.price
                    appendLine(" ${i + 1}. ${p.name}  ${qty}개  (₩${fmtMoney(revenue)})")
                }
            }
        }

        // [6] 회전율 최저
        appendLine()
        appendLine("[6] 🎯 재고 회전율 최저")
        if (lowestTurnover == null) {
            appendLine(" - 집계 없음")
        } else {
            val (prod, rate) = lowestTurnover
            appendLine(" - ${prod.name} (${fmtPct1(rate)})")
        }

        // [7] 주관 발주 우선순위
        appendLine()
        appendLine("[7] 🧭 주관 발주 우선순위 (가중치 기반)")
        if (reorderSubjective.isEmpty()) {
            appendLine(" - 없음")
        } else {
            reorderSubjective.take(5).forEachIndexed { i, (p, need, score) ->
                val scoreStr = String.format(Locale.getDefault(), "%.3f", score)
                appendLine(" ${i + 1}. ${p.name} +${need}개 (score=$scoreStr)")
            }
        }

        // [8] 주관 동적 할인 제안
        appendLine()
        appendLine("[8] 💡 주관 동적 할인 제안")
        val priced = pricingSubjective
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(5)
        if (priced.isEmpty()) {
            appendLine(" - 없음")
        } else {
            priced.forEach { (p, rate, discounted) ->
                val ratePct = String.format(Locale.getDefault(), "%.0f%%", rate * 100.0)
                appendLine(" - ${p.name}: $ratePct → ${fmtMoney(discounted)}원 (정가 ${fmtMoney(p.price)}원)")
            }
        }

        // [9] 종합 운영 현황
        appendLine()
        appendLine("[9] 📋 종합 운영 현황")
        if (operations.isEmpty()) {
            appendLine(" - 없음")
        } else {
            appendLine(
                pad("제품명", 18) + " " +
                        pad("분류", 8) + " " +
                        pad("재고(현/적)", 14) + " " +
                        pad("재고율", 8) + " " +
                        pad("판매", 6) + " " +
                        pad("매출", 10) + " " +
                        pad("회전율", 8) + " " +
                        "상태"
            )
            operations.forEach { r ->
                val statusBits = mutableListOf<String>()
                if (r.reorderNeed != null) statusBits.add("발주+${r.reorderNeed}")
                r.expiringDays?.let { if (it >= 0) statusBits.add("임박D-$it") }
                val status = statusBits.joinToString(", ")

                appendLine(
                    pad(r.name, 18) + " " +
                            pad(r.category.name, 8) + " " +
                            pad("${r.current}/${r.ideal}", 14) + " " +
                            pad(fmtPct1(r.stockRatePct), 8) + " " +
                            pad("${r.salesQty}", 6) + " " +
                            pad(fmtMoney(r.revenue), 10) + " " +
                            pad(fmtPct1(r.turnoverPct), 8) + " " +
                            status
                )
            }
        }

        // [10] 카테고리 세부 분류
        appendLine()
        appendLine("[10] 🗂 카테고리 세부 분류")

        // 음료: 용량(ml)
        if (drilldown.beverageByVolume.isEmpty()) {
            appendLine(" 음료(용량): - 없음")
        } else {
            appendLine(" 음료(용량별)")
            drilldown.beverageByVolume.forEach { (vol, items) ->
                val label = if (vol > 0) "${vol}ml" else "미지정"
                appendLine(groupSummaryLine("용량", label, items))
            }
        }

        // 식품: 유통기한 D-?
        if (drilldown.foodByDaysToExpire.isEmpty()) {
            appendLine(" 식품(유통기한): - 없음")
        } else {
            appendLine(" 식품(유통기한별)")
            drilldown.foodByDaysToExpire.forEach { (d, items) ->
                val label = if (d >= 0) "D-$d" else "만료지남"
                appendLine(groupSummaryLine("유통", label, items))
            }
        }

        // 생활용품: 브랜드
        if (drilldown.livingByBrand.isEmpty()) {
            appendLine(" 생활용품(브랜드): - 없음")
        } else {
            appendLine(" 생활용품(브랜드별)")
            drilldown.livingByBrand.forEach { (brand, items) ->
                val label = brand.ifBlank { "기타" }
                appendLine(groupSummaryLine("브랜드", label, items))
            }
        }
    }
}
