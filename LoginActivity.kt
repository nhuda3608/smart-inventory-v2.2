package bd.smartinventory.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import bd.smartinventory.app.databinding.ActivityLoginBinding

/**
 * v2.1 নতুন লগইন - ৩টি উপায়
 * 1. Google Sign-In
 * 2. Phone OTP
 * 3. পুরনো VBA স্টাইল ইউজার/পাসওয়ার্ড (HashPass compatible)
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        // 1. Google Login Button
        binding.btnGoogleLogin.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(this, gso)
            startActivityForResult(client.signInIntent, 1001)
        }

        // 2. Phone OTP Button -> OtpVerifyActivity
        binding.btnPhoneLogin.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            if (phone.length < 11) {
                Toast.makeText(this, "সঠিক মোবাইল নম্বর দিন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, OtpVerifyActivity::class.java).putExtra("phone", phone))
        }

        // 3. পুরনো সিস্টেম - VBA HashPass compatible
        binding.btnOldLogin.setOnClickListener {
            val user = binding.etUsername.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "ইউজার ও পাসওয়ার্ড দিন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // এখানে আপনার পুরনো modAuth.txt এর HashPass() লজিক কল হবে
            // HashPass(pass) == SysMessages থেকে হ্যাশ মিললে Dashboard এ যাবে
            if (legacyLoginCheck(user, pass)) {
                goDashboard(user, "Operator")
            } else {
                Toast.makeText(this, "লগইন ব্যর্থ!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // পুরনো VBA HashPass() এর Kotlin ভার্সন
    private fun hashPass(txt: String): String {
        var h = 5381.0
        val s = txt.trim().lowercase()
        for (c in s) {
            var code = c.code.toLong()
            if (code < 0) code += 65536
            h = h * 33 + code
            h -= (h / 1000000007).toLong() * 1000000007
        }
        h = h * 31 + s.length
        h -= (h / 1000000007).toLong() * 1000000007
        return h.toLong().toString()
    }

    private fun legacyLoginCheck(user: String, pass: String): Boolean {
        // Production: SysMessages শিট থেকে FindUserRow(user) করে hash মিলান
        // ডেমো তথ্য সরানো হয়েছে - শুধু আসল ইউজার ডেটা থেকে চেক হবে
        // TODO: Room DB বা Firebase থেকে ইউজার লোড করুন
        return false // আসল DB কানেক্ট না হওয়া পর্যন্ত
    }

    private fun goDashboard(user: String, role: String) {
        startActivity(Intent(this, DashboardActivity::class.java).apply {
            putExtra("user", user); putExtra("role", role)
        })
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val acc = task.result
                goDashboard(acc.displayName ?: acc.email ?: "Google User", "Admin")
            }
        }
    }
}
