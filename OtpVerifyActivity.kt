package bd.smartinventory.app

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bd.smartinventory.app.databinding.ActivityOtpVerifyBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class OtpVerifyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpVerifyBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        val phone = intent.getStringExtra("phone") ?: ""
        binding.tvPhone.text = "কোড পাঠানো হয়েছে: $phone"

        // Firebase Phone Auth শুরু
        sendOtp(phone)

        // Timer 60s
        startTimer()

        binding.btnVerify.setOnClickListener {
            val code = binding.etOtp.text.toString().trim()
            if (code.length != 6) {
                Toast.makeText(this, "৬ ডিজিট কোড দিন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyCode(code)
        }

        binding.btnResend.setOnClickListener {
            sendOtp(phone, true)
            startTimer()
        }

        // Auto-read hint - SMS Retriever API অটো ফিল করবে
        binding.etOtp.requestFocus()
    }

    private fun sendOtp(phone: String, isResend: Boolean = false) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+88$phone") // বাংলাদেশ +88
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        if (isResend && resendToken != null) {
            // resend with token
        }
        PhoneAuthProvider.verifyPhoneNumber(options)
        Toast.makeText(this, "OTP পাঠানো হচ্ছে...", Toast.LENGTH_SHORT).show()
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(cred: PhoneAuthCredential) {
            // Auto verification - কোড অটো বসবে
            cred.smsCode?.let { binding.etOtp.setText(it); verifyCode(it) }
        }
        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(this@OtpVerifyActivity, "ব্যর্থ: ${e.message}", Toast.LENGTH_LONG).show()
        }
        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
            verificationId = id
            resendToken = token
            Toast.makeText(this@OtpVerifyActivity, "কোড পাঠানো হয়েছে ✓", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyCode(code: String) {
        val id = verificationId
        if (id == null) {
            Toast.makeText(this, "OTP পাঠানো হয়নি, আবার চেষ্টা করুন", Toast.LENGTH_SHORT).show()
            return
        }
        val cred = PhoneAuthProvider.getCredential(id, code)
        auth.signInWithCredential(cred).addOnCompleteListener {
            if (it.isSuccessful) goDashboard()
            else Toast.makeText(this, "ভুল কোড!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goDashboard() {
        Toast.makeText(this, "লগইন সফল!", Toast.LENGTH_SHORT).show()
        startActivity(android.content.Intent(this, DashboardActivity::class.java).apply {
            putExtra("user", intent.getStringExtra("phone"))
            putExtra("role", "Operator")
        })
        finish()
    }

    private fun startTimer() {
        binding.btnResend.isEnabled = false
        object : CountDownTimer(60000, 1000) {
            override fun onTick(m: Long) { binding.tvTimer.text = "পুনরায় পাঠান (${m/1000}s)" }
            override fun onFinish() { binding.tvTimer.text = ""; binding.btnResend.isEnabled = true }
        }.start()
    }
}
