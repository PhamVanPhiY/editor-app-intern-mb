package com.example.editor_app_intern.ui.sticker

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.editor_app_intern.R
import com.example.editor_app_intern.SharedPreferences
import com.example.editor_app_intern.adapter.StickerAdapter
import com.example.editor_app_intern.constant.Constants.STICKER_DATA
import com.example.editor_app_intern.databinding.ActivityStickerBinding
import com.example.editor_app_intern.model.Sticker
import com.example.editor_app_intern.model.StickerLocal
import java.io.File
import java.util.UUID

class StickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStickerBinding
    private lateinit var stickerViewModel: StickerViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStickerBinding.inflate(layoutInflater)
        stickerViewModel = ViewModelProvider(this).get(StickerViewModel::class.java)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpView()
        stickerViewModel.stickers.observe(this) { stickers ->
            setUpStickerRecyclerView(stickers)
        }
        stickerViewModel.loadStickersFromJson()
    }

    private fun setUpView() {
        binding.apply {
            setSupportActionBar(toolbarSticker)
            supportActionBar?.title = resources.getString(R.string.sticker)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

    }

    private fun setUpStickerRecyclerView(stickerList: List<Sticker>) {
        val stickerAdapter = StickerAdapter(this@StickerActivity, stickerList) { sticker ->
            val localFile = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "${sticker.name}.png"
            )
            val stickerLocal = StickerLocal(
                id = UUID.randomUUID().toString(),
                path = localFile.absolutePath,
                widthSticker = 200f,
                heightSticker = 200f,
                x = 100f,
                y = 100f,
                false,
            )

            val sharedPrefs = SharedPreferences(this)
            sharedPrefs.saveSticker(stickerLocal)
            val resultIntent = Intent()
            resultIntent.putExtra(STICKER_DATA, stickerLocal)
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        binding.apply {
            rcvSticker.layoutManager =
                GridLayoutManager(this@StickerActivity, 2, GridLayoutManager.VERTICAL, false)
            rcvSticker.adapter = stickerAdapter
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
}