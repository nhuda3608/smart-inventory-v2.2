package bd.smartinventory.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bd.smartinventory.app.databinding.ActivitySaleEntryBinding

/**
 * v2.1 Sale Entry - FIXED: নিট লাভ = (বিক্রয় দাম - কেনা দাম) * পরিমাণ
 * আগে ভুল ছিল: শুধু বিক্রয় দামকেই লাভ ধরা হচ্ছিল
 * এখন ঠিক: 10 টাকায় কিনে 15 টাকায় বেচলে নিট লাভ = 5 টাকা
 * মোট বিক্রি (ক্যাশ বাক্স) = 15 টাকা আলাদা দেখাবে
 */
class SaleEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySaleEntryBinding

    // ProductMaster - এখন buyPrice + sellPrice দুটোই আছে
    private val products = listOf(
        Product("P001", "A4 কাগজ 80gsm", "রিম", 480.0, 550.0, 120),
        Product("P002", "Epson কালি 003", "পিস", 1050.0, 1250.0, 8),
        Product("P003", "লেমিনেটিং পাউচ", "পিস", 32.0, 45.0, 32),
        Product("P004", "ফটোকপি A4", "পিস", 2.0, 3.5, 500),
        Product("P005", "ছবি প্রিন্ট 4R", "পিস", 8.0, 15.0, 200)
    )

    data class Product(val code: String, val name: String, val unit: String, val buyPrice: Double, val sellPrice: Double, val stock: Int)
    data class CartItem(val product: Product, var qty: Int, var discount: Double = 0.0)

    private val cart = mutableListOf<CartItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaleEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val names = products.map { "${it.code} - ${it.name}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        binding.acProduct.setAdapter(adapter)
        binding.acProduct.threshold = 1

        binding.acProduct.setOnItemClickListener { _, _, pos, _ ->
            val p = products[pos]
            binding.tvPrice.text = "বিক্রয়: ৳ ${p.sellPrice} | কেনা: ৳ ${p.buyPrice} | লাভ: ৳ ${p.sellPrice - p.buyPrice}/পিস"
            binding.tvStock.text = "স্টক: ${p.stock} ${p.unit}"
            binding.etQty.requestFocus()
        }

        binding.btnAdd.setOnClickListener {
            val txt = binding.acProduct.text.toString().trim()
            if (txt.isEmpty()) {
                Toast.makeText(this, "পণ্য নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val product = findProduct(txt)
            if (product == null) {
                Toast.makeText(this, "নতুন পণ্য! দাম লিখে যোগ করুন", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val qty = binding.etQty.text.toString().toDoubleOrNull() ?: 0.0
            if (qty <= 0) {
                Toast.makeText(this, "পরিমাণ দিন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (qty > product.stock) {
                Toast.makeText(this, "স্টক কম! আছে ${product.stock}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val disc = binding.etDiscount.text.toString().toDoubleOrNull() ?: 0.0
            cart.add(CartItem(product, qty.toInt(), disc))
            refreshCart()
            clearEntry()
        }

        binding.btnSave.setOnClickListener {
            if (cart.isEmpty()) {
                Toast.makeText(this, "কার্ট খালি!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // FIXED হিসাব:
            val totalSales = cart.sumOf { it.product.sellPrice * it.qty - it.discount } // ক্যাশ বাক্সে জমা
            val totalCost = cart.sumOf { it.product.buyPrice * it.qty } // মোট কেনা খরচ
            val netProfit = totalSales - totalCost // নিট লাভ

            Toast.makeText(this, "সেভ ✓\nমোট বিক্রি (ক্যাশ): ৳ $totalSales\nনিট লাভ: ৳ $netProfit", Toast.LENGTH_LONG).show()
            // TODO: DailyLedger এ: Income = totalSales, Expense = totalCost, Profit = netProfit
            cart.clear()
            refreshCart()
        }

        binding.btnPrint.setOnClickListener {
            Toast.makeText(this, "ইনভয়েস PDF তৈরি হচ্ছে...", Toast.LENGTH_SHORT).show()
        }
        binding.btnScan.setOnClickListener {
            Toast.makeText(this, "ক্যামেরা খুলছে... (ML Kit Barcode)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findProduct(txt: String): Product? {
        return products.find { txt.contains(it.code, true) || txt.contains(it.name, true) }
    }

    private fun refreshCart() {
        if (cart.isEmpty()) {
            binding.tvCart.text = "কার্ট খালি - পণ্য যোগ করুন"
            binding.tvTotal.text = "মোট বিক্রি (ক্যাশ): ৳ ০\nনিট লাভ: ৳ ০"
            return
        }
        val sb = StringBuilder()
        for ((i, item) in cart.withIndex()) {
            val lineSales = item.product.sellPrice * item.qty - item.discount
            val lineProfit = (item.product.sellPrice - item.product.buyPrice) * item.qty - item.discount
            sb.append("${i+1}. ${item.product.name} x${item.qty} = ৳ $lineSales (লাভ ৳ $lineProfit)\n")
        }
        binding.tvCart.text = sb.toString()
        val totalSales = cart.sumOf { it.product.sellPrice * it.qty - it.discount }
        val netProfit = cart.sumOf { (it.product.sellPrice - it.product.buyPrice) * it.qty - it.discount }
        binding.tvTotal.text = "মোট বিক্রি (ক্যাশ): ৳ $totalSales\nনিট লাভ: ৳ $netProfit"
    }

    private fun clearEntry() {
        binding.acProduct.text.clear()
        binding.etQty.text.clear()
        binding.etDiscount.text.clear()
        binding.tvPrice.text = "৳ ০"
        binding.tvStock.text = ""
    }
}
