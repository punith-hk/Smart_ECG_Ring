package com.smartringpro.mannaheal.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import android.text.SpannableString
import android.text.style.UnderlineSpan
import androidx.appcompat.app.AppCompatActivity
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.register.RegisterRepository
import com.smartringpro.mannaheal.api.register.RegisterResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {
    private val registerRepository = RegisterRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etMobileNumber = findViewById<EditText>(R.id.etMobileNumber)
        val etName = findViewById<EditText>(R.id.etName)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)

        val text = "Already have an account?"
        val spannableString = SpannableString(text)
        spannableString.setSpan(UnderlineSpan(), 0, spannableString.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvSignIn.text = spannableString

        tvSignIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val mobileNumber = etMobileNumber.text.toString().trim()

            if (mobileNumber.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Please enter both name and mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (mobileNumber.length != 10) {
                Toast.makeText(this, "Mobile number must be 10 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerRepository.register(mobileNumber, name).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val registerResponse = response.body()!!

                        if (registerResponse.response == 0) {
                            // Success case: OTP sent
                            Toast.makeText(this@RegisterActivity, registerResponse.getFormattedMessage(), Toast.LENGTH_SHORT).show()

                            // Navigate to OtpActivity
                            val intent = Intent(this@RegisterActivity, OtpActivity::class.java)
                            intent.putExtra("user_id", registerResponse.user_id)
                            intent.putExtra("patient_id", registerResponse.patient_id)
                            intent.putExtra("name", name)
                            intent.putExtra("mobileNumber", mobileNumber)
                            intent.putExtra("new_account", true);
                            startActivity(intent)
                            finish()
                        } else {
                            // Error case: Show the error message
                            Toast.makeText(this@RegisterActivity, registerResponse.getFormattedMessage(), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // API call failed or returned an error
                        Toast.makeText(this@RegisterActivity, "Register failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    // Handle network failure
                    Toast.makeText(this@RegisterActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}

