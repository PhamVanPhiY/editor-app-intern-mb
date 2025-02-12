package com.example.editor_app_intern.ui.imagedetail

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.editor_app_intern.R
import com.example.editor_app_intern.SharedPreferences
import com.example.editor_app_intern.constant.Constants.FOLDER_IMAGE_SAVED
import com.example.editor_app_intern.constant.Constants.PATH_IMAGE_FROM_ALBUM
import com.example.editor_app_intern.databinding.ActivityImageDetailBinding
import com.example.editor_app_intern.ui.edit.EditActivity
import java.io.File
import java.io.FileOutputStream

class ImageDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageDetailBinding
    private lateinit var preferences: SharedPreferences
    private var imageUri: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferences = SharedPreferences(this)
        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpView()
        setUpGetUriImageFromAlbum()
    }

    private fun setUpView() {
        binding.apply {
            setSupportActionBar(toolbarHome)
            supportActionBar?.title = resources.getString(R.string.detail_image)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            btnDeleteImage.setOnClickListener {
                showDeleteConfirmationDialog()
            }

            btnEdit.setOnClickListener {
                preferences.clearImagePath()
                preferences.clearImagePathOrigin()
                imageUri?.let { it1 -> preferences.saveImagePathOrigin(it1) }
                imageUri?.let { it1 -> preferences.saveImagePath(it1) }
                val intent = Intent(this@ImageDetailActivity, EditActivity::class.java)
                startActivity(intent)
                finish()
            }
            btnShareImage.setOnClickListener { view ->
                val bitmapDrawable = binding.ivImageDetail.drawable as? BitmapDrawable
                val bitmap = bitmapDrawable?.bitmap
                bitmap?.let {
                    shareImageAndText(it)
                } ?: run {
                    Toast.makeText(
                        this@ImageDetailActivity,
                        "No image to share",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.question_confirm_detele_image)
            .setMessage(R.string.message_confirm_detele_image)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                deleteImage()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteImage() {
        if (imageUri != null) {
            val contentResolver = contentResolver
            try {
                val uriToDelete = Uri.parse(imageUri)
                Log.d("ImageDetailActivity", "Attempting to delete: $uriToDelete")
                val rowsDeleted = contentResolver.delete(uriToDelete, null, null)
                if (rowsDeleted > 0) {
                    Toast.makeText(this, R.string.delete_image_successfully, Toast.LENGTH_SHORT)
                        .show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, R.string.cannot_delete, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, R.string.cannot_delete, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, R.string.image_does_not_exist, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpGetUriImageFromAlbum() {
        imageUri = intent.getStringExtra(PATH_IMAGE_FROM_ALBUM)
        if (imageUri != null) {
            binding.apply {
                Glide.with(this@ImageDetailActivity).load(Uri.parse(imageUri)).into(ivImageDetail)
                val check = isImageOwnedByApp(Uri.parse(imageUri))
                Log.d("ImageDetailActivity", "Check image: $check")
                if (isImageOwnedByApp(Uri.parse(imageUri))) {
                    btnDeleteImage.isEnabled = true
                } else {
                    btnDeleteImage.isEnabled = false
                    btnDeleteImage.alpha = 0.5f
                }
            }
        }
        Log.d("ImageDetailActivity", "Image URI: $imageUri")
    }

    private fun isImageOwnedByApp(uri: Uri): Boolean {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val dataIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                val imagePath = it.getString(dataIndex)
                return imagePath.contains("/Pictures/${FOLDER_IMAGE_SAVED}/")
            }
        }
        return false
    }

    private fun shareImageAndText(bitmap: Bitmap) {
        val uri = getImageToShare(bitmap)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "This is a image i gift for you !!!")
            putExtra(Intent.EXTRA_SUBJECT, "Image for you")
            type = "image/png"
        }
        startActivity(Intent.createChooser(intent, "Share via"))
        finish()
    }

    private fun getImageToShare(bitmap: Bitmap): Uri? {
        val imageFolder = File(cacheDir, "images")
        var uri: Uri? = null

        try {
            if (!imageFolder.exists()) {
                imageFolder.mkdirs()
            }
            val file = File(imageFolder, "shared_image.png")
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                outputStream.flush()
            }
            uri = FileProvider.getUriForFile(
                this@ImageDetailActivity,
                "com.example.editor_app_intern",
                file
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return uri
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}