package com.example.editor_app_intern.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.editor_app_intern.R
import com.example.editor_app_intern.SharedPreferences
import com.example.editor_app_intern.databinding.ActivitySplashBinding
import com.example.editor_app_intern.ui.home.HomeActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = SharedPreferences(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        preferences.apply {
            clearImagePathOrigin()
            clearStickers()
            clearTextItems()
            clearImagePath()
            clearBackgroundBitmap()
            clearPaths()
        }
        setUpView()
    }

    private fun setUpView() {
        binding.apply {
            supportActionBar?.hide()
            Handler().postDelayed(
                {
                    startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                    finish()
                }, 3000
            )


        }
    }
}