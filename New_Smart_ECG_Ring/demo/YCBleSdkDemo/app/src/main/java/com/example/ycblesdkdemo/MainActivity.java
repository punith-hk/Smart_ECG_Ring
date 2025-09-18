package com.example.ycblesdkdemo;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No UI, just navigate to LoginActivity
        Intent intent = new Intent(this, com.example.ycblesdkdemo.ui.activities.LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
