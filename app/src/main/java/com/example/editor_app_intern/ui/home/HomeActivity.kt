package com.example.editor_app_intern.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.editor_app_intern.R
import com.example.editor_app_intern.databinding.ActivityHomeBinding
import com.example.editor_app_intern.ui.camera.CameraActivity

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpView()
    }

    private fun setUpView() {
        binding.apply {
            setSupportActionBar(toolbarHome)
            supportActionBar?.title = resources.getString(R.string.app_name)

            btnTakePhoto.setOnClickListener {
                startActivity(Intent(this@HomeActivity, CameraActivity::class.java))
            }
        }
    }

}