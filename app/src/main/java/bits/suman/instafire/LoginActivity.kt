package bits.suman.instafire

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import bits.suman.instafire.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "LoginActivity"
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val auth = FirebaseAuth.getInstance()
        if(auth.currentUser != null) {
            goPostsActivity()
        }

        binding.btnLogin.setOnClickListener {
            binding.btnLogin.isEnabled = false
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Email or Password cannot be blank", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task->
                binding.btnLogin.isEnabled = true
                if(task.isSuccessful) {
                    Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
                    goPostsActivity()
                } else {
                    Log.i(TAG, "sign in with email failed", task.exception)
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun goPostsActivity() {
        Log.i(TAG, "Go post activity")
        val intent = Intent(this, PostActivity::class.java)
        startActivity(intent)
        finish()
    }
}