package store

import java.time.LocalDate

fun main() {
    val today = LocalDate.now()

    // ⬇️ CSV에서 로드
    val products = Csv.loadProducts()   // src/main/resources/products.csv
    val sales    = Csv.loadSales()      // src/main/resources/sales.csv

    val manager = InventoryManager(
        products = products,
        thresholds = Thresholds(lowStockRatio = 0.30, expireSoonDays = 3),
        discountPolicy = DiscountPolicy(steps = listOf(1 to 0.40, 3 to 0.20, 7 to 0.10)),
        subjective = SubjectiveWeights(
            weightTurnover = 0.35,
            weightMargin = 0.25,
            weightStability = 0.25,
            weightFreshPriority = 0.15
        )
    )

    val report = manager.report(sales, today)
    println(report.render())
}
