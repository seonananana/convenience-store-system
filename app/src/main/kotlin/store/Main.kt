//샘플 데이터 주입->매니저 호출->보고서 출력
package store

import java.time.LocalDate

fun main() {
    val today = LocalDate.now()

    val products = mutableListOf(
        Product("콜라 500ml", ProductCategory.BEVERAGE, 1800, targetStock = 50, stock = 10),
        Product("감자칩 오리지널", ProductCategory.SNACK, 2200, targetStock = 40, stock = 18),
        Product("도시락 불고기", ProductCategory.FOOD, 4500, targetStock = 20, stock = 0, expiresAt = today.plusDays(2)),
        Product("건전지 AAA(4개입)", ProductCategory.LIVING, 3500, targetStock = 30, stock = 28)
    )

    val sales = listOf(
        Sale("콜라 500ml", 22),
        Sale("감자칩 오리지널", 15),
        Sale("건전지 AAA(4개입)", 3)
    )

    val manager = InventoryManager(
        products = products,
        thresholds = Thresholds(lowStockRatio = 0.30, expireSoonDays = 3),
        discountPolicy = DiscountPolicy(steps = listOf(1 to 0.40, 3 to 0.20, 7 to 0.10))
    )

    val report = manager.report(sales, today)
    println(report.render())
}
