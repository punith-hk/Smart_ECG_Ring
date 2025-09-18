package com.smartringpro.mannaheal.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.smartringpro.mannaheal.R

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.btnSubmit)
        loginButton.setOnClickListener {
            val intent = Intent(this, com.smartringpro.mannaheal.HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

