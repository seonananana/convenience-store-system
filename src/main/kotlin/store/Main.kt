// 샘플 데이터 주입 -> 매니저 호출 -> 보고서 출력
package store

import java.time.LocalDate

fun main() {
    val today = LocalDate.now()

    val products = listOf(
        Product("콜라 500ml", ProductCategory.BEVERAGE, price = 1800, currentStock = 10, idealStock = 50),
        Product("감자칩 오리지널", ProductCategory.SNACK, price = 2200, currentStock = 18, idealStock = 40),
        Product("도시락 불고기", ProductCategory.FOOD, price = 4500, currentStock = 0,  idealStock = 20, expiryDate = today.plusDays(2)),
        Product("건전지 AAA(4개입)", ProductCategory.LIVING, price = 3500, currentStock = 28, idealStock = 30)
    )

    val sales = listOf(
        Sale("콜라 500ml", 22),
        Sale("감자칩 오리지널", 15),
        Sale("건전지 AAA(4개입)", 3)
        // 도시락은 오늘 미판매(0) 가정
    )

    val manager = InventoryManager(
        products = products,
        thresholds = Thresholds(lowStockRatio = 0.30, expireSoonDays = 3),
        discountPolicy = DiscountPolicy(steps = listOf(1 to 0.40, 3 to 0.20, 7 to 0.10))
    )

    val report = manager.report(sales, today)
    println(report.render())
}
