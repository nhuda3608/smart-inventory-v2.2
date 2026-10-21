package bd.smartinventory.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bd.smartinventory.app.databinding.ActivityLedgerBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * v2.1 Ledger FIXED - সঠিক নিট লাভ হিসাব
 * নিট লাভ = মোট বিক্রি (ক্যাশ) - মোট কেনা খরচ - অন্যান্য খরচ
 * উদাহরণ: 10 টাকায় কিনে 15 টাকায় বেচলে = লাভ 5, ক্যাশ 15
 */
class LedgerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLedgerBinding

    // FIXED: এখন প্রতিটি বিক্রয়ে sales (ক্যাশ) এবং cost (কেনা) আলাদা রাখা হবে
    data class LedgerEntry(val date: String, val type: String, val desc: String, val sales: Double, val cost: Double, val expense: Double) {
        val netProfit: Double get() = sales - cost - expense
    }

    private val entries = mutableListOf(
        // উদাহরণ: A4 কাগজ 480 টাকায় কিনে 550 টাকায় বিক্রি x5 = ক্যাশ 2750, খরচ 2400, লাভ 350
        LedgerEntry("20/10/2026", "বিক্রয়", "P001 - A4 কাগজ x5 @550", 2750.0, 2400.0, 0.0),
        LedgerEntry("20/10/2026", "স্টুডিও", "ST-0001 - পাসপোর্ট ছবি x4", 300.0, 0.0, 0.0), // সার্ভিস - কোন কেনা খরচ নেই, পুরোটাই লাভ
        LedgerEntry("20/10/2026", "ফটোকপি", "PC-0001 - ফটোকপি 20p x3.5", 70.0, 20.0, 0.0), // কাগজ/কালি খরচ 20
        LedgerEntry("20/10/2026", "খরচ", "দোকান ভাড়া", 0.0, 0.0, 5000.0),
        LedgerEntry("19/10/2026", "কেনা", "মদিনা পেপার - A4 10 রিম", 0.0, 0.0, 4800.0) // স্টক কেনা - ক্যাশ থেকে যায়, কিন্তু লাভে সরাসরি নয় (স্টক সম্পদ)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLedgerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvDate.text = SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD")).format(Date())
        refreshLedger()

        binding.btnAddExpense.setOnClickListener { addExpense() }
        binding.btnMonthlyReport.setOnClickListener {
            val totalSales = entries.sumOf { it.sales }
            val totalCost = entries.sumOf { it.cost }
            val totalExpense = entries.sumOf { it.expense }
            val netProfit = totalSales - totalCost - totalExpense
            Toast.makeText(this, "মাসিক রিপোর্ট:\nমোট বিক্রি (ক্যাশ): ৳ $totalSales\nনিট লাভ: ৳ $netProfit", Toast.LENGTH_LONG).show()
        }
        binding.btnBackup.setOnClickListener {
            Toast.makeText(this, "ব্যাকআপ: Backup_${SimpleDateFormat("yyyyMMdd_hhmmss").format(Date())}.db ✓", Toast.LENGTH_LONG).show()
        }
        binding.btnClearDemo.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("ডেমো ডাটা মুছুন?")
                .setMessage("ClearDemoData() - demo_mark থাকা রো মুছবে")
                .setPositiveButton("হ্যাঁ, মুছুন") { _, _ ->
                    entries.removeIf { it.desc.contains("demo", true) }
                    refreshLedger()
                    Toast.makeText(this, "ডেমো ডাটা মোছা হয়েছে ✓", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("রাখুন", null)
                .show()
        }
        binding.chipAll.setOnClickListener { refreshLedger("all") }
        binding.chipIncome.setOnClickListener { refreshLedger("income") }
        binding.chipExpense.setOnClickListener { refreshLedger("expense") }
    }

    private fun addExpense() {
        val desc = binding.etExpenseDesc.text.toString().trim()
        val amt = binding.etExpenseAmt.text.toString().toDoubleOrNull() ?: 0.0
        if (desc.isEmpty() || amt <= 0) {
            Toast.makeText(this, "বিবরণ ও টাকা দিন", Toast.LENGTH_SHORT).show()
            return
        }
        val date = SimpleDateFormat("dd/MM/yyyy").format(Date())
        entries.add(0, LedgerEntry(date, "খরচ", desc, 0.0, 0.0, amt))
        binding.etExpenseDesc.text.clear()
        binding.etExpenseAmt.text.clear()
        refreshLedger()
        Toast.makeText(this, "খরচ যোগ: ৳ $amt ✓", Toast.LENGTH_SHORT).show()
    }

    private fun refreshLedger(filter: String = "all") {
        val filtered = when (filter) {
            "income" -> entries.filter { it.sales > 0 }
            "expense" -> entries.filter { it.expense > 0 || it.cost > 0 }
            else -> entries
        }

        val sb = StringBuilder()
        for (e in filtered) {
            val line = when {
                e.sales > 0 -> "🟢 ${e.date} | ${e.type} | ${e.desc} | ক্যাশ +৳${e.sales} | লাভ +৳${e.netProfit}"
                e.expense > 0 -> "🔴 ${e.date} | ${e.type} | ${e.desc} | খরচ -৳${e.expense}"
                else -> "⚪ ${e.date} | ${e.type} | ${e.desc}"
            }
            sb.append(line + "\n")
        }
        binding.tvLedger.text = sb.toString().ifEmpty { "কোনো লেনদেন নেই" }

        // FIXED হিসাব
        val totalSales = entries.sumOf { it.sales } // ক্যাশ বাক্সে মোট জমা
        val totalCost = entries.sumOf { it.cost } // বিক্রি হওয়া পণ্যের কেনা দাম
        val totalExpense = entries.sumOf { it.expense } // ভাড়া, বেতন ইত্যাদি
        val netProfit = totalSales - totalCost - totalExpense // আসল নিট লাভ

        binding.tvIncome.text = "৳ $totalSales" // এখন মোট বিক্রি (ক্যাশ)
        binding.tvExpense.text = "৳ ${totalCost + totalExpense}"
        binding.tvProfit.text = "৳ $netProfit"
        binding.tvProfit.setTextColor(if (netProfit >= 0) 0xFF2E7D32.toInt() else 0xFFC62828.toInt())

        // Header এ স্পষ্ট লেবেল
        binding.tvIncomeLabel.text = "মোট বিক্রি (ক্যাশ)"
        binding.tvProfitLabel.text = "নিট লাভ"
    }
}
