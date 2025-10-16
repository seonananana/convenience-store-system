package store

data class Sale(val productName: String, val qty: Int)
data class PurchaseOrder(val productName: String, val qty: Int)

/** 콘솔 ‘종합 운영 현황’ 1행 모델 */
data class OperationRow(
    val name: String,
    val category: ProductCategory,
    val price: Int,
    val current: Int,
    val ideal: Int,
    val stockRatePct: Double,   // current / ideal * 100 (ideal=0이면 100)
    val salesQty: Int,          // 집계기간 판매수량
    val revenue: Int,           // salesQty * price
    val turnoverPct: Double,    // salesQty / ideal * 100 (ideal=0이면 0)
    val expiringDays: Int?,     // 남은 유통기한(D-?)
    val reorderNeed: Int?       // 자동발주 필요수량(없으면 null)
)
