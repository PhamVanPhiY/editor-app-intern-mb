package com.example.editor_app_intern.ui.result

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.editor_app_intern.R
import com.example.editor_app_intern.SharedPreferences
import com.example.editor_app_intern.constant.Constants.IS_EDIT_AGAIN
import com.example.editor_app_intern.constant.Constants.PATH_IMAGE_JUST_SAVED
import com.example.editor_app_intern.databinding.ActivityResultBinding
import com.example.editor_app_intern.ui.camera.CameraActivity
import com.example.editor_app_intern.ui.edit.EditActivity
import com.example.editor_app_intern.ui.home.HomeActivity

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var preferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = SharedPreferences(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpGetImage()
        setUpView()
    }

    private fun setUpView() {
        binding.apply {
            btnCloseApp.setOnClickListener {
                super.onBackPressed()
            }
            btnEditAgain.setOnClickListener {
                val intent = Intent(this@ResultActivity, EditActivity::class.java)
                intent.putExtra(IS_EDIT_AGAIN, true)
                startActivity(intent)
                finish()
            }
            btnCloseApp.setOnClickListener {
                val builder = AlertDialog.Builder(this@ResultActivity)
                builder.setTitle("Exit Application")
                builder.setMessage("Are you sure you want to exit?")
                builder.setPositiveButton("Yes") { dialog, which ->
                    preferences.clearStickers()
                    preferences.clearTextItems()
                    preferences.clearImagePath()
                    preferences.clearBackgroundBitmap()
                    preferences.clearImagePathOrigin()
                    preferences.clearPaths()
                    finishAffinity()
                }

                builder.setNegativeButton("No") { dialog, which ->
                    dialog.dismiss()
                }

                val dialog = builder.create()
                dialog.show()
            }
            btnHome.setOnClickListener {
                preferences.clearStickers()
                preferences.clearTextItems()
                preferences.clearImagePath()
                preferences.clearBackgroundBitmap()
                preferences.clearImagePathOrigin()
                preferences.clearPaths()
                val intent = Intent(this@ResultActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setUpGetImage() {
        val imageUriString = intent.getStringExtra(PATH_IMAGE_JUST_SAVED)
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            binding.ivResult.setImageURI(imageUri)
        }
    }
}