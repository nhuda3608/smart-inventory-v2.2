package bd.smartinventory.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bd.smartinventory.app.databinding.ActivityDashboardBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * v2.1 নতুন ড্যাশবোর্ড - আপনার VBA Dashboard এর আধুনিক রূপ
 * পুরনো: আজকের বিক্রয়, মাসের লাভ, স্টকের মূল্য, কম স্টক
 * নতুন: + চার্ট + ক্যালেন্ডার + দ্রুত বাটন
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = intent.getStringExtra("user") ?: "Admin"
        val role = intent.getStringExtra("role") ?: "Admin"

        // Header
        binding.tvWelcome.text = "স্বাগতম, $user ($role)"
        binding.tvDate.text = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("bn", "BD")).format(Date())

        // ===== 4টি কার্ড - VBA Dashboard এর মতো =====
        // এগুলো আপনার Excel এর DailyLedger, ProductMaster থেকে আসবে
        loadCards()

        // ===== 12 মাসের চার্ট =====
        setupChart()

        // ===== কম স্টক এলার্ট =====
        setupLowStockList()

        // ===== দ্রুত বাটন =====
        binding.btnNewSale.setOnClickListener {
            Toast.makeText(this, "নতুন বিক্রয় খুলছে...", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, SaleEntryActivity::class.java))
        }
        binding.btnStockEntry.setOnClickListener {
            Toast.makeText(this, "স্টক এন্ট্রি খুলছে...", Toast.LENGTH_SHORT).show()
        }
        binding.btnInvoice.setOnClickListener {
            Toast.makeText(this, "ইনভয়েস প্রিন্ট...", Toast.LENGTH_SHORT).show()
        }
        binding.btnStudioIncome.setOnClickListener {
            Toast.makeText(this, "স্টুডিও আয়...", Toast.LENGTH_SHORT).show()
        }

        // Calendar click
        binding.calendarView.setOnDateChangeListener { _, y, m, d ->
            Toast.makeText(this, "তারিখ: $d/${m+1}/$y - বিক্রয় দেখুন", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCards() {
        // FIXED: নিট লাভ ও মোট বিক্রি (ক্যাশ) আলাদা
        // উদাহরণ: 10 টাকায় কিনে 15 টাকায় বেচলে ক্যাশ 15, লাভ 5
        val todaySales = 12500.0 // মোট বিক্রি (ক্যাশ বাক্স)
        val todayCost = 8200.0 // কেনা খরচ
        val todayNetProfit = todaySales - todayCost // নিট লাভ = 4300

        binding.cardTodaySale.tvValue.text = "৳ ${String.format("%,.0f", todaySales)}"
        binding.cardTodaySale.tvLabel.text = "মোট বিক্রি (ক্যাশ)"
        binding.cardTodaySale.tvSub.text = "নিট লাভ: ৳ ${String.format("%,.0f", todayNetProfit)}"

        binding.cardMonthlyProfit.tvValue.text = "৳ ৪৫,২০০"
        binding.cardMonthlyProfit.tvLabel.text = "মাসের নিট লাভ"
        binding.cardMonthlyProfit.tvSub.text = "মোট বিক্রি: ৳ ৮৯,০০০ | লক্ষ্য ৳ ৬০,০০০"

        binding.cardStockValue.tvValue.text = "৳ ২,৮০,০০০"
        binding.cardStockValue.tvLabel.text = "স্টকের মূল্য (কেনা দামে)"
        binding.cardStockValue.tvSub.text = "১২৪টি পণ্য"

        binding.cardLowStock.tvValue.text = "৮টি"
        binding.cardLowStock.tvLabel.text = "কম স্টক"
        binding.cardLowStock.tvSub.text = "এখনই কিনুন!"
    }

    private fun setupChart() {
        val months = arrayOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগ", "সেপ্ট", "অক্টো", "নভে", "ডিসে")
        val income = listOf(45000f, 52000f, 48000f, 61000f, 55000f, 67000f, 72000f, 68000f, 74000f, 81000f, 79000f, 85000f)
        val expense = listOf(28000f, 31000f, 29000f, 35000f, 32000f, 38000f, 40000f, 39000f, 41000f, 45000f, 43000f, 46000f)

        val entriesIncome = income.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
        val entriesExpense = expense.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }

        val setIncome = BarDataSet(entriesIncome, "আয়").apply { color = 0xFF2E7D32.toInt() }
        val setExpense = BarDataSet(entriesExpense, "ব্যয়").apply { color = 0xFFE53935.toInt() }

        binding.barChart.data = BarData(setIncome, setExpense).apply { barWidth = 0.4f }
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(months)
        binding.barChart.xAxis.granularity = 1f
        binding.barChart.description.isEnabled = false
        binding.barChart.groupBars(0f, 0.15f, 0.05f)
        binding.barChart.invalidate()
    }

    private fun setupLowStockList() {
        // Demo low stock - ProductMaster থেকে আসবে
        val lowStock = listOf(
            "A4 কাগজ (৫০০) - স্টক: ১২ রিম",
            "Epson কালি - স্টক: ৩ পিস",
            "লেমিনেটিং পাউচ - স্টক: ৮ পিস"
        )
        binding.tvLowStockList.text = lowStock.joinToString("\n• ", prefix = "• ")
    }
}
