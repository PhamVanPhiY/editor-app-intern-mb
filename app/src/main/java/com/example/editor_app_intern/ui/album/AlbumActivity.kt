package com.example.editor_app_intern.ui.album

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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


            btnMenu.setOnClickListener {
                val dialogView =
                    LayoutInflater.from(this@AlbumActivity).inflate(R.layout.custom_dialog, null)
                val builder = MaterialAlertDialogBuilder(this@AlbumActivity)
                    .setView(dialogView)

                val dialog = builder.create()

                dialogView.findViewById<Button>(R.id.btn_latest).setOnClickListener {
                    viewModel.sortImagesByNewest()
                    dateAdapter.updateDates(viewModel.dates.value ?: emptyList())
                    dialog.dismiss()
                }

                dialogView.findViewById<Button>(R.id.btn_oldest).setOnClickListener {
                    viewModel.sortImagesByOldest()
                    dateAdapter.updateDates(viewModel.dates.value ?: emptyList())
                    dialog.dismiss()
                }


                dialog.show()
            }
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
    
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            val folderPath = FOLDER_IMAGE_EDITED
            viewModel.loadImagesFromFolder(folderPath)
        }
    }

}