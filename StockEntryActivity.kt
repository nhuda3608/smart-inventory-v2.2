package bd.smartinventory.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bd.smartinventory.app.databinding.ActivityStockEntryBinding

/**
 * v2.1 Stock Entry - আপনার VBA StockEntry.txt এর আধুনিক রূপ
 * পুরনো: কোড/নাম লিখলেই প্রোডাক্ট খুঁজে পাওয়া, QuickAddProduct, স্টক বাড়া
 * নতুন: বারকোড + সাপ্লায়ার + কেনা দাম + বিক্রয় দাম + অটো স্টক আপডেট
 */
class StockEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockEntryBinding

    data class ProductStock(val code: String, val name: String, val unit: String, var stock: Int, var buyPrice: Double, var sellPrice: Double)

    // Demo ProductMaster - Room DB থেকে লোড হবে
    private val products = mutableListOf(
        ProductStock("P001", "A4 কাগজ 80gsm", "রিম", 120, 480.0, 550.0),
        ProductStock("P002", "Epson কালি 003", "পিস", 8, 1050.0, 1250.0),
        ProductStock("P003", "লেমিনেটিং পাউচ", "পিস", 32, 32.0, 45.0)
    )

    data class StockCart(val product: ProductStock, var qty: Int, var buyPrice: Double)

    private val cart = mutableListOf<StockCart>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Supplier list - CustomerMaster এর মতো
        val suppliers = listOf("আলম ট্রেডার্স", "মদিনা পেপার", "খান স্টেশনারি", "নতুন সাপ্লায়ার +")
        binding.acSupplier.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suppliers))

        // Product AutoComplete - VBA FindProductRow() এর মতো
        val names = products.map { "${it.code} - ${it.name}" }
        binding.acProduct.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names))
        binding.acProduct.threshold = 1

        binding.acProduct.setOnItemClickListener { _, _, pos, _ ->
            val p = products[pos]
            binding.etBuyPrice.setText(p.buyPrice.toString())
            binding.etSellPrice.setText(p.sellPrice.toString())
            binding.tvCurrentStock.text = "বর্তমান স্টক: ${p.stock} ${p.unit}"
            binding.etQty.requestFocus()
        }

        binding.btnAdd.setOnClickListener {
            val txt = binding.acProduct.text.toString().trim()
            var product = findProduct(txt)

            // QuickAddProduct - নতুন পণ্য হলে
            if (product == null && txt.isNotEmpty()) {
                val buy = binding.etBuyPrice.text.toString().toDoubleOrNull() ?: 0.0
                val sell = binding.etSellPrice.text.toString().toDoubleOrNull() ?: 0.0
                if (buy <= 0 || sell <= 0) {
                    Toast.makeText(this, "নতুন পণ্যের কেনা/বেচা দাম দিন", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val newCode = "P%03d".format(products.size + 1)
                product = ProductStock(newCode, txt, "পিস", 0, buy, sell)
                products.add(product)
                Toast.makeText(this, "নতুন পণ্য যোগ: $newCode - $txt ✓", Toast.LENGTH_SHORT).show()
            }

            if (product == null) {
                Toast.makeText(this, "পণ্য নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val qty = binding.etQty.text.toString().toIntOrNull() ?: 0
            if (qty <= 0) {
                Toast.makeText(this, "পরিমাণ দিন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val buy = binding.etBuyPrice.text.toString().toDoubleOrNull() ?: product.buyPrice

            cart.add(StockCart(product, qty, buy))
            refreshCart()
            clearEntry()
        }

        binding.btnSave.setOnClickListener {
            if (cart.isEmpty()) {
                Toast.makeText(this, "কার্ট খালি!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val supplier = binding.acSupplier.text.toString().ifEmpty { "অজানা" }
            var total = 0.0
            for (item in cart) {
                item.product.stock += item.qty // স্টক বাড়া - VBA এর মূল কাজ
                item.product.buyPrice = item.buyPrice
                total += item.qty * item.buyPrice
            }
            val sellPriceUpdate = binding.etSellPrice.text.toString().toDoubleOrNull()
            if (sellPriceUpdate != null && cart.size == 1) {
                cart[0].product.sellPrice = sellPriceUpdate
            }

            Toast.makeText(this, "স্টক সেভ: $supplier থেকে ৳ $total ✓\nস্টক আপডেট হয়েছে", Toast.LENGTH_LONG).show()
            // TODO: Room DB + DailyLedger এ কেনা হিসাব যোগ
            cart.clear()
            refreshCart()
        }

        binding.btnScan.setOnClickListener {
            Toast.makeText(this, "বারকোড স্ক্যান... (ML Kit)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findProduct(txt: String): ProductStock? {
        return products.find { txt.contains(it.code, true) || txt.contains(it.name, true) }
    }

    private fun refreshCart() {
        if (cart.isEmpty()) {
            binding.tvCart.text = "কার্ট খালি - পণ্য যোগ করুন"
            binding.tvTotal.text = "মোট কেনা: ৳ ০"
            return
        }
        val sb = StringBuilder()
        for ((i, item) in cart.withIndex()) {
            sb.append("${i+1}. ${item.product.name} x${item.qty} @৳${item.buyPrice} = ৳${item.qty * item.buyPrice}\n")
        }
        binding.tvCart.text = sb.toString()
        binding.tvTotal.text = "মোট কেনা: ৳ ${cart.sumOf { it.qty * it.buyPrice }}"
    }

    private fun clearEntry() {
        binding.acProduct.text.clear()
        binding.etQty.text.clear()
        binding.tvCurrentStock.text = ""
    }
}
