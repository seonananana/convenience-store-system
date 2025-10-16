//판매/입출고 기록을 위한 경량 모델
package store

data class Sale(val productName: String, val qty: Int)           // 금일 판매
data class PurchaseOrder(val productName: String, val qty: Int)  // 발주 제안용
