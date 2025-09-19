package com.smartringpro.mannaheal.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.smartringpro.mannaheal.R

class OtpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        val submitButton = findViewById<Button>(R.id.btnSubmit)
        submitButton.setOnClickListener {
            val intent = Intent(this, com.smartringpro.mannaheal.HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

