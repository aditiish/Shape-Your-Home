package com.app.interiordesign.account

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.app.interiordesign.HomeActivity
import com.app.interiordesign.R
import com.app.interiordesign.databinding.ActivityLoginBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val TAG = LoginActivity::class.java.simpleName
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
//        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
//        supportActionBar?.setCustomView(R.layout.abs_layout)
//        supportActionBar!!.elevation = 0.0f

        // Welcome gif
        Glide.with(this).asGif()
            .load("https://media1.tenor.com/images/e9a2b43613bdde8dea94e81c4ca7e4c2/tenor.gif?itemid=5072286")
            .apply(RequestOptions.circleCropTransform())
            .into(binding.kotlinImageView)

        binding.loginButtonLogin.setOnClickListener {
            performLogin()
        }

        binding.backToRegisterTextview.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left)
    }

    private fun performLogin() {
        val email = binding.emailEdittextLogin.text.toString()
        val password = binding.passwordEdittextLogin.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            return
        }
        showLoadingView()
        // Back to register
        binding.backToRegisterTextview.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    hideLoadingView()
                    return@addOnCompleteListener
                }
                Log.d(TAG, "Successfully logged in: ${it.result!!.user!!.uid}")

                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                overridePendingTransition(R.anim.enter, R.anim.exit)
            }
            .addOnFailureListener {
                Toast.makeText(this, "${it.message}", Toast.LENGTH_SHORT).show()
                hideLoadingView()
            }
    }

    private fun showLoadingView() {
        binding.backToRegisterTextview.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE
    }

    private fun hideLoadingView() {
        binding.backToRegisterTextview.visibility = View.VISIBLE
        binding.loadingView.visibility = View.GONE
    }
}
