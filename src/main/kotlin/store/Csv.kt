package store

import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

object Csv {
    private fun readTable(resourceName: String): Pair<List<String>, List<List<String>>> {
        val url = Csv::class.java.classLoader.getResource(resourceName)
            ?: error("Resource not found: $resourceName")
        BufferedReader(InputStreamReader(url.openStream())).use { br ->
            val lines = br.readLines().filter { it.isNotBlank() }.map { it.trim() }
            if (lines.isEmpty()) error("Empty CSV: $resourceName")

            val header = lines.first().split(',').map { it.trim().lowercase() }
            val rows = lines.drop(1).map { row -> row.split(',').map { it.trim() } }
            return header to rows
        }
    }

    private fun idx(header: List<String>, vararg keys: String): Int? {
        val cands = keys.map { it.lowercase() }.toSet()
        return header.indexOfFirst { it in cands }.takeIf { it >= 0 }
    }

    fun loadProducts(resourceName: String = "products.csv"): List<Product> {
        val (h, rows) = readTable(resourceName)

        val iName   = idx(h, "name") ?: error("products.csv header needs 'name'")
        val iCat    = idx(h, "category", "cat") ?: error("products.csv header needs 'category'")
        val iPrice  = idx(h, "price") ?: error("products.csv header needs 'price'")

        // 다양한 현장 표기 지원: current|stock, ideal|target
        val iCurrent= idx(h, "current", "stock") ?: error("products.csv header needs 'current' or 'stock'")
        val iIdeal  = idx(h, "ideal", "target") ?: error("products.csv header needs 'ideal' or 'target'")
        val iExpiry = idx(h, "expiry", "expire", "exp", "expirydate")

        return rows.map { c ->
            val name = c.getOrNull(iName) ?: ""
            val cat  = ProductCategory.valueOf((c.getOrNull(iCat) ?: "SNACK").uppercase())
            val price = (c.getOrNull(iPrice) ?: "0").toInt()
            val current = (c.getOrNull(iCurrent) ?: "0").toInt()
            val ideal   = (c.getOrNull(iIdeal) ?: "0").toInt()
            val expiry  = iExpiry?.let { idx -> c.getOrNull(idx)?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } }
            Product(name, cat, price, current, ideal, expiry)
        }
    }

    fun loadSales(resourceName: String = "sales.csv"): List<Sale> {
        val (h, rows) = readTable(resourceName)
        val iName = idx(h, "name", "product", "productname") ?: error("sales.csv header needs 'name'")
        val iQty  = idx(h, "qty", "quantity", "count", "sold") ?: error("sales.csv header needs 'qty'")

        // date 컬럼은 선택
        return rows.map { c ->
            val name = c.getOrNull(iName) ?: ""
            val qty  = (c.getOrNull(iQty) ?: "0").toInt()
            Sale(name, qty)
        }
    }
}
