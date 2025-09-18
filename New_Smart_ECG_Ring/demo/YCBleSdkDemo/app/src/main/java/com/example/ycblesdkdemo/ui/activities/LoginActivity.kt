package com.example.ycblesdkdemo.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.example.ycblesdkdemo.R

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.btn_login)
        loginButton.setOnClickListener {
            val intent = Intent(this, com.example.ycblesdkdemo.HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

