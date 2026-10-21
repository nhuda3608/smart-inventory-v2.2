package bd.smartinventory.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bd.smartinventory.app.databinding.ActivityIncomeBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * v2.1 Income - Studio + Photocopy আলাদা আয় (modIncome.txt এর ২টি শিট)
 * NewStudioEntry / NewPhotoEntry / SaveIncomeEntry / PrintIncomeReport
 * এখন এক Activity দিয়ে দুটোই - type = "ST" বা "PC"
 */
class IncomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomeBinding
    private var type: String = "ST" // ST = Studio, PC = Photocopy

    data class IncomeRow(val date: String, val slip: String, val customer: String, val work: String, val qty: Double, val rate: Double, val total: Double, val discount: Double, val net: Double)

    private val rows = mutableListOf<IncomeRow>()
    private var slipNo = 1

    // Studio works
    private val studioWorks = listOf("পাসপোর্ট ছবি 4R", "স্টুডিও ছবি", "লেমিনেটিং", "বায়োডাটা", "ভিডিও এডিট")
    // Photocopy works
    private val photoWorks = listOf("ফটোকপি A4", "ফটোকপি A3", "প্রিন্ট A4", "স্ক্যান", "কম্পোজ", "বাঁধাই")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        type = intent.getStringExtra("type") ?: "ST"
        val title = if (type == "ST") "📸 স্টুডিও আয়" else "📄 ফটোকপি আয়"
        binding.tvTitle.text = title
        binding.tvSlip.text = "স্লিপ: ${nextSlip()}"
        binding.tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val works = if (type == "ST") studioWorks else photoWorks
        binding.acWork.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, works))

        // Customer auto - CustomerMaster
        val customers = listOf("Walk-in", "রহিম", "করিম", "আলম স্টোর")
        binding.acCustomer.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, customers))

        binding.btnAdd.setOnClickListener { addRow() }
        binding.btnSave.setOnClickListener { saveAll() }
        binding.btnPrint.setOnClickListener {
            Toast.makeText(this, "প্রিন্ট প্রিভিউ... (PrintIncomeReport)", Toast.LENGTH_SHORT).show()
        }
        binding.btnNew.setOnClickListener { newSlip() }
    }

    private fun nextSlip(): String {
        val prefix = if (type == "ST") "ST" else "PC"
        return "$prefix-%04d".format(slipNo)
    }

    private fun addRow() {
        val work = binding.acWork.text.toString().trim()
        val qty = binding.etQty.text.toString().toDoubleOrNull() ?: 0.0
        val rate = binding.etRate.text.toString().toDoubleOrNull() ?: 0.0
        val disc = binding.etDiscount.text.toString().toDoubleOrNull() ?: 0.0

        if (work.isEmpty()) {
            Toast.makeText(this, "কাজের নাম দিন", Toast.LENGTH_SHORT).show()
            return
        }
        if (qty <= 0) {
            Toast.makeText(this, "পরিমাণ দিন", Toast.LENGTH_SHORT).show()
            return
        }
        if (rate <= 0) {
            Toast.makeText(this, "দর দিন", Toast.LENGTH_SHORT).show()
            return
        }

        val total = qty * rate
        val net = total - disc
        val customer = binding.acCustomer.text.toString().ifEmpty { "Walk-in" }
        val date = binding.tvDate.text.toString()

        rows.add(IncomeRow(date, nextSlip(), customer, work, qty, rate, total, disc, net))
        refreshList()

        // Clear entry row
        binding.acWork.text.clear()
        binding.etQty.text.clear()
        binding.etRate.text.clear()
        binding.etDiscount.text.clear()
        binding.acWork.requestFocus()
    }

    private fun refreshList() {
        if (rows.isEmpty()) {
            binding.tvList.text = "কোনো এন্ট্রি নেই - উপরে কাজ যোগ করুন"
            binding.tvTotal.text = "মোট: ৳ ০"
            return
        }
        val sb = StringBuilder()
        for ((i, r) in rows.withIndex()) {
            sb.append("${i+1}. ${r.work} x${r.qty} @৳${r.rate} = ৳${r.total} -৳${r.discount} = ৳${r.net}\n")
        }
        binding.tvList.text = sb.toString()
        binding.tvTotal.text = "মোট: ৳ ${rows.sumOf { it.net }}"
    }

    private fun saveAll() {
        if (rows.isEmpty()) {
            Toast.makeText(this, "কোনো লাইন নেই!", Toast.LENGTH_SHORT).show()
            return
        }
        val slip = nextSlip()
        val total = rows.sumOf { it.net }
        // TODO: Room DB তে সেভ - StudioIncome / PhotocopyIncome শিটে
        // BumpCounter + NextFreeRow
        Toast.makeText(this, "$slip সেভ: ৳ $total ✓", Toast.LENGTH_LONG).show()
        slipNo++
        rows.clear()
        refreshList()
        binding.tvSlip.text = "স্লিপ: ${nextSlip()}"
        binding.acCustomer.text.clear()
    }

    private fun newSlip() {
        rows.clear()
        refreshList()
        binding.tvSlip.text = "স্লিপ: ${nextSlip()}"
        Toast.makeText(this, "নতুন স্লিপ: ${nextSlip()}", Toast.LENGTH_SHORT).show()
    }
}
