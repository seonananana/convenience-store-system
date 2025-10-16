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

    /** 이름에서 "500ml", "1.5L" 등을 추정 */
    private fun guessVolumeMlFromName(name: String): Int? {
        val ml = Regex("""(\d{2,5})\s*ml""", RegexOption.IGNORE_CASE).find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (ml != null) return ml
        val l = Regex("""(\d+(?:\.\d+)?)\s*l""", RegexOption.IGNORE_CASE).find(name)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        return l?.let { (it * 1000).toInt() }
    }

    fun loadProducts(resourceName: String = "products.csv"): List<Product> {
        val (h, rows) = readTable(resourceName)

        val iName   = idx(h, "name") ?: error("products.csv header needs 'name'")
        val iCat    = idx(h, "category", "cat") ?: error("products.csv header needs 'category'")
        val iPrice  = idx(h, "price") ?: error("products.csv header needs 'price'")
        val iCurrent= idx(h, "current", "stock") ?: error("products.csv header needs 'current' or 'stock'")
        val iIdeal  = idx(h, "ideal", "target") ?: error("products.csv header needs 'ideal' or 'target'")
        val iExpiry = idx(h, "expiry", "expire", "exp", "expirydate")
        val iVol    = idx(h, "volume", "volume_ml", "vol", "ml")   // 옵션
        val iBrand  = idx(h, "brand", "maker")                     // 옵션

        return rows.map { c ->
            val name = c.getOrNull(iName) ?: ""
            val cat  = ProductCategory.valueOf((c.getOrNull(iCat) ?: "SNACK").uppercase())
            val price = (c.getOrNull(iPrice) ?: "0").toInt()
            val current = (c.getOrNull(iCurrent) ?: "0").toInt()
            val ideal   = (c.getOrNull(iIdeal) ?: "0").toInt()
            val expiry  = iExpiry?.let { k -> c.getOrNull(k)?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } }

            val volFromCsv = iVol?.let { k -> c.getOrNull(k)?.takeIf { it.isNotBlank() }
                ?.lowercase()?.replace("ml","")?.replace(" ","") }
                ?.let { raw ->
                    raw.toIntOrNull() ?: raw.removeSuffix("l").toDoubleOrNull()?.let { (it * 1000).toInt() }
                }
            val vol = volFromCsv ?: guessVolumeMlFromName(name)
            val brand = iBrand?.let { k -> c.getOrNull(k)?.takeIf { it.isNotBlank() } }

            Product(name, cat, price, current, ideal, expiry, volumeMl = vol, brand = brand)
        }
    }

    fun loadSales(resourceName: String = "sales.csv"): List<Sale> {
        val (h, rows) = readTable(resourceName)
        val iName = idx(h, "name", "product", "productname") ?: error("sales.csv header needs 'name'")
        val iQty  = idx(h, "qty", "quantity", "count", "sold") ?: error("sales.csv header needs 'qty'")
        return rows.map { c ->
            val name = c.getOrNull(iName) ?: ""
            val qty  = (c.getOrNull(iQty) ?: "0").toInt()
            Sale(name, qty)
        }
    }
}
