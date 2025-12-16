package com.example.myappnominoichi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton


class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        findViewById<MaterialButton>(R.id.btnDrink).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnGuide).setOnClickListener {
            startActivity(Intent(this, GuideActivity::class.java))
        }
    }
}

