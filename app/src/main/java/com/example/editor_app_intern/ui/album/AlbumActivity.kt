package com.example.editor_app_intern.ui.album

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.editor_app_intern.R
import com.example.editor_app_intern.adapter.DateAdapter
import com.example.editor_app_intern.constant.Constants.FOLDER_IMAGE_EDITED
import com.example.editor_app_intern.databinding.ActivityAlbumBinding

class AlbumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlbumBinding
    private var isViewStubInflated = false
    private var emptyStateView: View? = null
    private val viewModel: AlbumViewModel by viewModels()
    private lateinit var dateAdapter: DateAdapter

    companion object {
        const val REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpView()
        setUpRecyclerViewImage()
        val folderPath = FOLDER_IMAGE_EDITED
        viewModel.loadImagesFromFolder(folderPath)
    }

    private fun setUpView() {
        binding.apply {
            setSupportActionBar(toolbarHome)
            supportActionBar?.title = resources.getString(R.string.album_photo)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setUpRecyclerViewImage() {
        binding.apply {
            rcvMain.layoutManager = LinearLayoutManager(this@AlbumActivity)

            viewModel.dates.observe(this@AlbumActivity) { dates ->
                if (dates.isEmpty()) {
                    if (!isViewStubInflated) {
                        emptyStateView = emptyStateLayout.inflate()
                        emptyStateLayout.visibility = View.VISIBLE
                        rcvMain.visibility = View.GONE
                        isViewStubInflated = true
                    }
                } else {
                    emptyStateLayout.visibility = View.GONE
                    rcvMain.visibility = View.VISIBLE
                    isViewStubInflated = false
                    emptyStateView = null
                    dateAdapter = DateAdapter(dates)
                    rcvMain.adapter = dateAdapter
                }
            }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            val folderPath = FOLDER_IMAGE_EDITED
            viewModel.loadImagesFromFolder(folderPath)
        }
    }

}