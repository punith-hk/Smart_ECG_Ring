package com.smartringpro.mannaheal.ui.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.otp.OtpRepository
import com.smartringpro.mannaheal.api.otp.OtpResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtpActivity : AppCompatActivity() {

    private val otpRepository = OtpRepository()

    private var userId: Int? = null
    private var patientId: Int? = null
    private var name: String? = null
    private var mobileNumber: String? = null
    private var isNewAccount: Boolean = false

    private lateinit var tvResendOtp: TextView
    private lateinit var tvOtpMessage: TextView
    private lateinit var etOtp: EditText
    private lateinit var btnSubmit: Button
    private lateinit var countDownTimer: CountDownTimer
    private var timeLeftInMillis: Long = 60000
    private var isTimerRunning = false
    private lateinit var backIcon: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        userId = intent.getIntExtra("user_id", -1)
        patientId = intent.getIntExtra("patient_id", -1)
        name = intent.getStringExtra("name")
        mobileNumber = intent.getStringExtra("mobileNumber")

        tvResendOtp = findViewById(R.id.tvResendOtp)
        etOtp = findViewById<EditText>(R.id.etOtp)
        btnSubmit = findViewById<Button>(R.id.btnSubmit)
        tvOtpMessage = findViewById<TextView>(R.id.tvOtpMessage)


        isNewAccount = intent.getBooleanExtra("new_account", false)

        backIcon = findViewById<ImageButton>(R.id.backIcon)
        backIcon.setOnClickListener {
            if (isNewAccount) {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
            finish()
        }

        val message = "Otp Sent to your Mobile Number $mobileNumber"
        tvOtpMessage.text = message

        startCountDown()

        btnSubmit.setOnClickListener {
            val otp = etOtp.text.toString().trim()

            if (otp.isEmpty()) {
                Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (otp.length != 6) {
                Toast.makeText(this, "OTP must be 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            otpRepository.validateOtp(userId, otp).enqueue(object : Callback<OtpResponse> {
                override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val otpResponse = response.body()!!
                        if (otpResponse.response == 0) {
                            Toast.makeText(
                                this@OtpActivity, "OTP verified successfully!", Toast.LENGTH_SHORT
                            ).show()
                            saveOtpResponseDetails(otpResponse)
                            if (isNewAccount) {
                                val intent = Intent(this@OtpActivity, RegisterProfileActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                val intent = Intent(this@OtpActivity, HomeActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            Toast.makeText(
                                this@OtpActivity,
                                otpResponse.message ?: "OTP verification failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@OtpActivity, "OTP validation failed", Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                    // Handle failure to call API
                    Toast.makeText(this@OtpActivity, "Error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }

        tvResendOtp.setOnClickListener {
            if (!isTimerRunning) {
                timeLeftInMillis = 60000
                startCountDown()
                Toast.makeText(
                    this@OtpActivity,
                    "OTP resent successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveOtpResponseDetails(otpResponse: OtpResponse) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save individual values to SharedPreferences
        editor.putString("accessToken", otpResponse.accessToken)
        editor.putString("user", otpResponse.user)
        editor.putString("email", otpResponse.email)
        editor.putString("roleCode", otpResponse.role_code)
        editor.putString("mobileNumber", otpResponse.mobile_number)
        editor.putInt("id", otpResponse.id ?: -1)
        editor.putBoolean("isLoggedIn", true)

        // Apply the changes to SharedPreferences
        editor.apply()
    }

    private fun startCountDown() {
        isTimerRunning = true
        updateResendOtpText(timeLeftInMillis)

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateResendOtpText(timeLeftInMillis)
            }

            override fun onFinish() {
                isTimerRunning = false
                updateResendOtpText(0)
            }
        }.start()
    }

    private fun updateResendOtpText(timeLeft: Long) {
        val message: String
        val spannableString: SpannableString

        if (timeLeft > 0) {
            message = "Resend OTP in ${timeLeft / 1000} seconds"
            spannableString = SpannableString(message)

            val start = message.indexOf("${timeLeft / 1000}")
            val end = start + "${timeLeft / 1000}".length

            spannableString.setSpan(
                StyleSpan(Typeface.BOLD), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableString.setSpan(
                ForegroundColorSpan(Color.BLACK),
                start,
                end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

        } else {
            message = "Resend OTP"
            spannableString = SpannableString(message)
            val start = 0
            val end = message.length
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#408FFD")),
                start,
                end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                UnderlineSpan(), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tvResendOtp.text = spannableString
    }
}

