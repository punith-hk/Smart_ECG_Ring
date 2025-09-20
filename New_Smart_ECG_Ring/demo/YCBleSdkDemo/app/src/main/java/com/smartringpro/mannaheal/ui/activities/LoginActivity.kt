package com.smartringpro.mannaheal.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.text.style.StyleSpan
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.login.LoginRepository
import com.smartringpro.mannaheal.api.login.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private val loginRepository = LoginRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.hide()

        val etMobileNumber = findViewById<EditText>(R.id.etMobileNumber)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        val text = "Don’t have an account? Sign Up"
        val spannableString = SpannableString(text)

        val start = text.indexOf("Sign Up")
        val end = start + "Sign Up".length

        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#408FFD")), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvSignUp.text = spannableString

        tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnSubmit.setOnClickListener {
            val mobileNumber = etMobileNumber.text.toString().trim()

            if (mobileNumber.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (mobileNumber.length != 10) {
                Toast.makeText(this, "Mobile number must be 10 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(mobileNumber)
        }
    }

    private fun login(mobileNumber: String) {
        loginRepository.login(mobileNumber).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    val message = loginResponse.getFormattedMessage()

                    if (loginResponse.response == 0) {
                        // Success case
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, OtpActivity::class.java)
                        intent.putExtra("mobileNumber", mobileNumber)
                        intent.putExtra("user_id", loginResponse.user_id)
                        intent.putExtra("new_account", false);
                        startActivity(intent)
                        finish()
                    } else {
                        // Error case
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // API call failed but returned a non-successful response
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // Handle network or other failures
                Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

