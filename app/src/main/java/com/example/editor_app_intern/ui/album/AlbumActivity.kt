package com.example.editor_app_intern.ui.album

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.editor_app_intern.R
import com.example.editor_app_intern.adapter.DateAdapter
import com.example.editor_app_intern.constant.Constants.PATH_IMAGE_FROM_ALBUM
import com.example.editor_app_intern.databinding.ActivityAlbumBinding
import com.example.editor_app_intern.dialog.NotificationDialog
import com.example.editor_app_intern.dialog.OptionDialog
import com.example.editor_app_intern.extension.atLeastVersionUpSideDownCake
import com.example.editor_app_intern.helper.PermissionHelper.PERMISSIONS
import com.example.editor_app_intern.interfaces.OnImageClickListener
import com.example.editor_app_intern.ui.imagedetail.ImageDetailActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AlbumActivity : AppCompatActivity(), OnImageClickListener {
    private lateinit var binding: ActivityAlbumBinding
    private var isViewStubInflated = false
    private var emptyStateView: View? = null
    private val viewModel: AlbumViewModel by viewModels()
    private lateinit var dateAdapter: DateAdapter
    private lateinit var permissionsRequestLauncher: ActivityResultLauncher<Array<String>>
    private var isResumeEnable = false

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
        setLauncher()
        setUpView()
        setUpRecyclerViewImage()
        checkPermissionsAndLoadImages()

    }

    private fun checkPermissionsAndLoadImages() {
        if (hasStoragePermission()) {
            viewModel.loadImagesFromGallery()
        } else {
            permissionsRequestLauncher.launch(PERMISSIONS)
        }
    }

    private fun setLauncher() {
        permissionsRequestLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionRequest(permissions)
        }
    }

    private fun handlePermissionRequest(permissions: Map<String, Boolean>) {
        val granted = atLeastVersionUpSideDownCake {
            permissions.values.any { it }
        }?.let {
            permissions.values.all { it }
        } ?: false || hasStoragePermission()

        if (granted)
            viewModel.loadImagesFromGallery()
        else {
            showPermissionDialog(permissions)
        }
    }

    private fun showPermissionDialog(permissions: Map<String, Boolean>) {
        when {
            PERMISSIONS.any { shouldShowRequestPermissionRationale(it) } -> {
                NotificationDialog(
                    title = getString(R.string.permission_required),
                    message = getString(R.string.storage_permission_is_required_to_access_images),
                    labelPositive = getString(R.string.ok),
                    onPositive = {
                        permissionsRequestLauncher.launch(PERMISSIONS)
                    }
                ).show(supportFragmentManager, "PermissionDialog")
            }

            PERMISSIONS.any { !shouldShowRequestPermissionRationale(it) } -> {
                showPermanentDenialDialog()
            }

            else -> {
                permissionsRequestLauncher.launch(PERMISSIONS)
            }
        }
    }

    private fun showPermanentDenialDialog() {
        val dialog = OptionDialog(
            title = getString(R.string.permission_required),
            message = getString(R.string.permission_denied_permanently_dialog),
            labelPositive = getString(R.string.ok),
            labelNegative = getString(R.string.cancel),
            onPositive = {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
                isResumeEnable = true
            },
            onNegative = {
                showPermanentDenialDialog()
            }
        )
        dialog.show(supportFragmentManager, "PermissionDialog")
    }

    private fun hasStoragePermission(): Boolean {
        return atLeastVersionUpSideDownCake {
            PERMISSIONS.any {
                checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        } ?: PERMISSIONS.all {
            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
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
                    dateAdapter = DateAdapter(dates, this@AlbumActivity)
                    rcvMain.adapter = dateAdapter
                }
            }
        }
    }

    override fun onImageClick(imageUri: String) {
        val intent = Intent(this, ImageDetailActivity::class.java).apply {
            putExtra(PATH_IMAGE_FROM_ALBUM, imageUri)
        }
        startActivityForResult(intent, REQUEST_CODE)
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
            viewModel.loadImagesFromGallery()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isResumeEnable) {
            if (!hasStoragePermission()) {
                showPermanentDenialDialog()
            } else {
                checkPermissionsAndLoadImages()
            }
            viewModel.loadImagesFromGallery()
        }
    }

}