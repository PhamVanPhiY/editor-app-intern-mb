package com.example.editor_app_intern.ui.edit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.editor_app_intern.R
import com.example.editor_app_intern.SharedPreferences
import com.example.editor_app_intern.adapter.FontAdapter
import com.example.editor_app_intern.constant.Constants.IS_EDIT_AGAIN
import com.example.editor_app_intern.constant.Constants.PATH_IMAGE_JUST_SAVED
import com.example.editor_app_intern.customeview.PaintView
import com.example.editor_app_intern.databinding.ActivityEditBinding
import com.example.editor_app_intern.dialog.NotificationDialog
import com.example.editor_app_intern.dialog.OptionDialog
import com.example.editor_app_intern.extension.atLeastVersionUpSideDownCake
import com.example.editor_app_intern.helper.PermissionHelper.PERMISSIONS
import com.example.editor_app_intern.model.FontItem
import com.example.editor_app_intern.model.TextItem
import com.example.editor_app_intern.ui.album.AlbumActivity
import com.example.editor_app_intern.ui.result.ResultActivity
import com.example.editor_app_intern.ui.sticker.StickerActivity
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.github.dhaval2404.colorpicker.model.ColorSwatch
import com.yalantis.ucrop.UCrop
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    private var isBackgroundRemoved = false
    private var textX: Float = 400f
    private var textY: Float = 600f
    private var textCount: Int = 0
    private lateinit var preferences: SharedPreferences
    private lateinit var gpuImage: GPUImage
    private lateinit var hueFilter: GPUImageHueFilter
    private lateinit var editViewModel: EditViewModel
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var permissionsRequestLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var singlePhotoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var progressBarRemoveBackground: ProgressBar
    private var fontPath: String = "fonts/rubik_regular.ttf"
    private var colorText: Int = -16777216
    private var sizeText: Float = 40f
    private var textXEdit: Float = 0f
    private var textYEdit: Float = 0f
    private var savedImageURI: String? = null
    private var isCheckDraw = false
    private lateinit var imagePath: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        editViewModel = ViewModelProvider(this).get(EditViewModel::class.java)

        editViewModel.fontList.observe(this, { fontList ->
            setUpFontRecyclerView(fontList)
        })


        gpuImage = GPUImage(this)
        hueFilter = GPUImageHueFilter(0f)
        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        progressBarRemoveBackground = binding.progressBar
        preferences = SharedPreferences(this)
        binding.paintView.loadTextItems(preferences)
        val isEditAgain = intent.getBooleanExtra(IS_EDIT_AGAIN, false)
        if (isEditAgain) {
            loadImageEditAgain()
        }
        imagePath = preferences.getImagePath().toString()
        val backgroundBitmap = preferences.getBackgroundBitmap()
        Log.d("EditActivity", "backgroundBitmap: $backgroundBitmap")
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled) {
            binding.paintView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val paintViewWidth = binding.paintView.width
                    val paintViewHeight = binding.paintView.height

                    if (paintViewWidth > 0 && paintViewHeight > 0) {
                        val scaledBackgroundBitmap = Bitmap.createScaledBitmap(
                            backgroundBitmap,
                            paintViewWidth,
                            paintViewHeight,
                            true
                        )
                        binding.paintView.updateBackgroundBitmap(scaledBackgroundBitmap)
                        Log.d(
                            "EditActivity",
                            "Scaled Background Bitmap size: ${scaledBackgroundBitmap.width} x ${scaledBackgroundBitmap.height}"
                        )
                        binding.paintView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            })
        } else if (imagePath.isNotEmpty()) {
            val imageUri: Uri = if (imagePath.startsWith("content://")) {
                Uri.parse(imagePath)
            } else {
                Uri.fromFile(File(imagePath.replace("file:", "")))
            }

            try {
                val inputStream = contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap == null) {
                    Log.e("EditActivity", "Failed to decode bitmap from path: $imagePath")
                    return
                }

                binding.paintView.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val paintViewWidth = binding.paintView.width
                        val paintViewHeight = binding.paintView.height

                        if (paintViewWidth > 0 && paintViewHeight > 0) {
                            val scaledBitmap = Bitmap.createScaledBitmap(
                                originalBitmap,
                                paintViewWidth,
                                paintViewHeight,
                                true
                            )
                            binding.paintView.updateBackgroundBitmap(scaledBitmap)
                            Log.d(
                                "EditActivity",
                                "Scaled Bitmap size: ${scaledBitmap.width} x ${scaledBitmap.height}"
                            )
                            binding.paintView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                })
            } catch (e: FileNotFoundException) {
                Log.e("EditActivity", "File not found: ${e.message}")
            } catch (e: Exception) {
                Log.e("EditActivity", "Error loading bitmap: ${e.message}")
            }
        } else {
            Log.e("EditActivity", "Invalid image path and no background bitmap available.")
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpLauncher()
        setUpView()
        upLoadPhotoFromPhotoPicker()
        setUpGetSticker()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun setUpGetSticker() {
        preferences = SharedPreferences(this)
        val savedStickers = preferences.getStickers()
        binding.paintView.stickerItems.addAll(savedStickers)
    }

    @SuppressLint("ResourceType", "ClickableViewAccessibility")
    private fun setUpView() {
        binding.apply {
            btnDraw.setOnClickListener {
                paintView.isEraserEnabled = false
                isCheckDraw = !isCheckDraw

                if (isCheckDraw) {
                    paintView.isDrawingEnabled = true
                    binding.lineDraw.visibility = View.VISIBLE
                } else {
                    paintView.isDrawingEnabled = false
                    binding.lineDraw.visibility = View.INVISIBLE
                }
            }

            btnEraser.setOnClickListener {
                paintView.clearCanvas()
                preferences.clearPaths()
            }

            btnUndo.setOnClickListener {
                paintView.undoDrawing()
            }

            btnRedo.setOnClickListener {
                paintView.redoDrawing()
            }

            btnRemoveBackground.setOnClickListener {
                if (!isBackgroundRemoved) {
                    val bitmap = paintView.canvasBitmap
                    if (bitmap != null) {
                        progressBarRemoveBackground.visibility = View.VISIBLE
                        paintView.removeBackground(bitmap, progressBarRemoveBackground)
                        isBackgroundRemoved = true
                    }
                } else {
                    paintView.undoRemoveBackground()
                    isBackgroundRemoved = false

                }
            }

            btnPickColor.setOnClickListener {
                setBrushColor(this@EditActivity, paintView)
            }


            btnSelectImage.setOnClickListener {
                preferences.savePaths(paintView.paths)
                Log.d("EditActivity", "Path list from sp ${preferences.getPaths()}")
                launchPhoToPicker()
            }
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            btnText.setOnClickListener {
                openInputText()
            }

            btnColor.setOnClickListener {
                setColorForText()
            }
            btnDone.setOnClickListener {
                val slideDownAnimation =
                    AnimationUtils.loadAnimation(this@EditActivity, R.anim.slide_down)
                layoutInputText.visibility = ConstraintLayout.INVISIBLE
                layoutInputText.startAnimation(slideDownAnimation)
                inputMethodManager.hideSoftInputFromWindow(tvInputText.windowToken, 0)
                val textEntered = tvInputText.text.toString()
                if (textEntered.isNotEmpty()) {
                    val textXNew: Float
                    val textYNew: Float

                    if (textXEdit > 0 && textYEdit > 0) {
                        textXNew = textXEdit
                        textYNew = textYEdit
                    } else {
                        textXNew = textX
                        textYNew = textY + (textCount * 100F)
                    }

                    paintView.addText(
                        id = UUID.randomUUID().toString(),
                        textEntered,
                        textXNew,
                        textYNew,
                        fontPath,
                        colorText,
                        sizeText
                    )
                    textCount++
                }

                tvInputText.text.clear()
                paintView.isTextBoxVisible = true
                paintView.saveTextItems(preferences)
            }

            seekbarSizeText.max = 100
            seekbarSizeText.progress = 40
            seekbarSizeText.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                @SuppressLint("SetTextI18n")
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        CoroutineScope(Dispatchers.Main).launch {
                            withContext(Dispatchers.IO) {
                                sizeText = progress.toFloat()
                                paintView.updateSelectedTextSize(progress.toFloat())
                            }
                            binding.tvSizeText.text = progress.toString()

                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })
            seekbarHue.max = 360
            seekbarHue.progress = 180
            seekbarHue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                private var lastProgress = 180

                @SuppressLint("SetTextI18n")
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    lastProgress = if (progress > 180) {
                        progress - 360
                    } else {
                        progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    CoroutineScope(Dispatchers.Default).launch {
                        hueFilter.setHue(lastProgress.toFloat())
                        applyHueFilter()
                    }
                }
            })


            btnSave.setOnClickListener {
                preferences.savePaths(paintView.paths)

                paintView.isTextBoxVisible = false
                paintView.isStickerTextBoxVisible = false
                paintView.selectedTextItem = null
                paintView.selectedStickerItem = null

                paintView.viewTreeObserver.addOnPreDrawListener(object :
                    ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        paintView.viewTreeObserver.removeOnPreDrawListener(this)

                        CoroutineScope(Dispatchers.IO).launch {
                            paintView.backgroundBitmap?.let { bitmap ->
                                preferences.saveBackgroundBitmap(bitmap)
                            }

                            val savedImageURI = saveImageAndCopyToDirectory().toString()

                            withContext(Dispatchers.Main) {
                                val intent =
                                    Intent(this@EditActivity, ResultActivity::class.java).apply {
                                        putExtra(PATH_IMAGE_JUST_SAVED, savedImageURI)
                                    }
                                startActivity(intent)
                                finish()
                            }
                        }
                        return true
                    }
                })

                paintView.invalidate()
            }

            btnHue.setOnClickListener {
                openEditHue()
            }

            btnSticker.setOnClickListener {
                preferences.savePaths(paintView.paths)
                startActivity(Intent(this@EditActivity, StickerActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

            btnCrop.setOnClickListener {
                val imagePathOrigin = preferences.getImagePathOrigin()
                preferences.savePaths(paintView.paths)
                if (imagePathOrigin != null) {
                    if (imagePathOrigin.isNotEmpty()) {
                        val imageUri: Uri
                        if (imagePathOrigin.startsWith("content://")) {
                            imageUri = Uri.parse(imagePathOrigin)
                        } else {
                            imageUri = Uri.fromFile(File(imagePathOrigin.replace("file:", "")))
                        }
                        startCrop(imageUri)
                    } else {
                        Toast.makeText(this@EditActivity, "Invalid image path", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

        }
    }

    @SuppressLint("ResourceType")
    private fun setColorForText() {
        MaterialColorPickerDialog.Builder(this@EditActivity).setTitle(R.string.pick_color)
            .setColorShape(ColorShape.CIRCLE).setColorSwatch(ColorSwatch._300)
            .setDefaultColor(Color.BLACK).setColorListener { color, _ ->
                binding.apply {
                    tvInputText.setTextColor(color)
                    paintView.setBrushColorForText(color)
                    colorText = color
                    Log.d("EditActivity", "Selected color: $color")
                }
            }.show()
    }

    private fun setUpFontRecyclerView(fontList: List<FontItem>) {
        val fontAdapter = FontAdapter(fontList) { font ->
            applyFont(font)
        }
        binding.rcvFont.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.rcvFont.adapter = fontAdapter
    }

    private fun applyFont(font: FontItem) {
        binding.apply {
            tvInputText.setTypeface(Typeface.createFromAsset(assets, font.fontPath))
            paintView.setFont(font.fontPath)
            fontPath = font.fontPath
        }
    }

    private fun loadImageEditAgain() {
        binding.paintView.apply {
            backgroundBitmap = preferences.getBackgroundBitmap()
            loadStickerItems()
            loadTextItems()
            invalidate()
        }
    }

    private fun startCrop(imageUri: Uri) {
        val destinationFileName = "cropped_image.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))
        UCrop.of(imageUri, destinationUri)
            .withMaxResultSize(1920, 1080)
            .start(this)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            if (resultUri != null) {
                preferences.clearImagePath()
                resultUri.path?.let { preferences.saveImagePath(it) }
                val imageCropped = BitmapFactory.decodeFile(resultUri.path)

                if (imageCropped != null) {
                    binding.paintView.updateBackgroundBitmap(imageCropped)
                    preferences.saveBackgroundBitmap(imageCropped)
                } else {
                    Log.e("EditActivity", "Unable to decode bitmap from the cropped image URI.")
                }
            } else {
                Log.e("EditActivity", "Result URI is null.")
            }


        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
        }
    }

    private fun applyHueFilter() {

        if (binding.paintView.backgroundBitmap != null) {
            CoroutineScope(Dispatchers.Default).launch {
                gpuImage.setImage(binding.paintView.backgroundBitmap)
                gpuImage.setFilter(hueFilter)
                val filteredBitmap = gpuImage.bitmapWithFilterApplied
                withContext(Dispatchers.Main) {
                    if (filteredBitmap != null && filteredBitmap.width > 0 && filteredBitmap.height > 0) {
                        binding.paintView.backgroundBitmap = filteredBitmap
                        binding.paintView.updateBackgroundBitmap(filteredBitmap)
                        preferences.saveBackgroundBitmap(filteredBitmap)
                    } else {
                        Log.e(
                            "EditActivity",
                            "Filtered Bitmap is either null or has invalid dimensions"
                        )
                        Toast.makeText(
                            this@EditActivity,
                            R.string.fail_to_apply_hue_filter,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setUpLauncher() {
        permissionsRequestLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionRequest(permissions)
        }
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

    private fun handlePermissionRequest(permissions: Map<String, Boolean>) {
        val granted = atLeastVersionUpSideDownCake {
            permissions.values.any { it }
        }?.let {
            permissions.values.all { it }
        } ?: false || hasStoragePermission()

        if (granted)
            startActivity(Intent(this@EditActivity, AlbumActivity::class.java))
        else {
            when {
                PERMISSIONS.any {
                    shouldShowRequestPermissionRationale(it)
                } -> {
                    NotificationDialog(
                        title = getString(R.string.permission_required),
                        message = getString(R.string.storage_permission_is_required_to_access_images),
                        labelPositive = getString(R.string.ok),
                        onPositive = {
                            permissionsRequestLauncher.launch(PERMISSIONS)
                        }
                    ).show(supportFragmentManager, "PermissionDialog")
                }

                PERMISSIONS.any {
                    !shouldShowRequestPermissionRationale(it)
                } -> {
                    OptionDialog(
                        title = getString(R.string.permission_required),
                        message = getString(R.string.permission_denied_permanently_dialog),
                        labelPositive = getString(R.string.ok),
                        labelNegative = getString(R.string.cancel),
                        onPositive = {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            })
                        },
                    ).show(supportFragmentManager, "PermissionDialog")
                }

                else -> {
                    permissionsRequestLauncher.launch(PERMISSIONS)
                }
            }
        }
    }

    fun openInputText() {
        binding.apply {
            paintView.isTextBoxVisible = true
            layoutInputText.visibility = ConstraintLayout.VISIBLE
            val slideUpAnimation = AnimationUtils.loadAnimation(this@EditActivity, R.anim.slide_up)

            layoutInputText.startAnimation(slideUpAnimation)
            tvInputText.isEnabled = true
            tvInputText.requestFocus()

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(tvInputText, InputMethodManager.SHOW_IMPLICIT)

            tvEnteredText.visibility = View.GONE
        }
    }

    fun openEditHue() {
        binding.apply {
            val slideUpAnimation = AnimationUtils.loadAnimation(this@EditActivity, R.anim.slide_up)
            layoutEditHue.visibility = ConstraintLayout.VISIBLE
            layoutEditHue.startAnimation(slideUpAnimation)

        }
    }

    fun closeEditHue() {
        binding.apply {
            layoutEditHue.visibility = ConstraintLayout.INVISIBLE
        }
    }

    fun openInputTextEdit(text: String, x: Float, y: Float) {
        binding.apply {
            val slideUpAnimation = AnimationUtils.loadAnimation(this@EditActivity, R.anim.slide_up)
            layoutInputText.visibility = ConstraintLayout.VISIBLE
            layoutInputText.startAnimation(slideUpAnimation)
            tvInputText.setText(text)
            tvInputText.isEnabled = true
            tvInputText.requestFocus()
            inputMethodManager.showSoftInput(tvInputText, InputMethodManager.SHOW_IMPLICIT)

            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            rootView.viewTreeObserver.addOnGlobalLayoutListener {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val keyboardHeight = rootView.height - rect.height()
                if (keyboardHeight > 200) {
                    buttonContainer.translationY = -(keyboardHeight - 200).toFloat()
                }
            }
            textXEdit = x
            textYEdit = y
        }
    }

    fun openEditSizeText() {
        binding.apply {
            val slideUpAnimation = AnimationUtils.loadAnimation(this@EditActivity, R.anim.slide_up)
            layoutEditSizeText.visibility = ConstraintLayout.VISIBLE
            layoutEditSizeText.startAnimation(slideUpAnimation)
        }
    }

    fun closeEditSizeText() {
        binding.apply {
            val slideDownAnimation =
                AnimationUtils.loadAnimation(this@EditActivity, R.anim.slide_down)
            layoutEditSizeText.visibility = ConstraintLayout.INVISIBLE
            layoutEditSizeText.startAnimation(slideDownAnimation)
            paintView.isEditingText = false
        }

    }

    @SuppressLint("SetTextI18n")
    fun setupSeekBarForText(selectedTextItem: TextItem) {
        binding.apply {
            seekbarSizeText.progress = selectedTextItem.size.toInt()
            binding.tvSizeText.text = selectedTextItem.size.toInt().toString()
        }

    }

    private fun saveImageAndCopyToDirectory(): Uri? {
        binding.apply {
            val bitmap = paintView.canvasBitmap ?: return null
            val imageName = "edited_image_${System.currentTimeMillis()}.jpg"

            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.WIDTH, bitmap.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/photo_editor_app")
                }
            }

            return try {
                contentResolver.insert(imageCollection, contentValues)?.let { uri ->
                    contentResolver.openOutputStream(uri).use { outputStream ->
                        if (outputStream != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

                            lifecycleScope.launch(Dispatchers.Main) {
                                Toast.makeText(
                                    this@EditActivity,
                                    R.string.save_image_successfully,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            val directory = File(
                                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "photo_editor_app"
                            )
                            if (!directory.exists()) {
                                directory.mkdirs()
                            }

                            val file = File(directory, imageName)
                            FileOutputStream(file).use { fileOutputStream ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                            }
                            Uri.fromFile(file)
                        } else {
                            throw IOException("Cannot open output stream")
                        }
                    }
                } ?: throw IOException("Cannot create record in media store")
            } catch (e: IOException) {
                e.printStackTrace()

                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditActivity,
                        R.string.fail_to_save_image,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                null
            }
        }
    }


    @SuppressLint("ResourceType")
    private fun setBrushColor(context: Context, paintView: PaintView) {
        MaterialColorPickerDialog.Builder(context).setTitle(R.string.pick_color)
            .setColorShape(ColorShape.CIRCLE).setColorSwatch(ColorSwatch._300)
            .setDefaultColor(Color.BLACK).setColorListener { color, colorHex ->
                paintView.setBrushColor(color);
                binding.lineDraw.setBackgroundColor(color)
            }.show()
    }

    @SuppressLint("ResourceType")
    private fun upLoadPhotoFromPhotoPicker() {
        singlePhotoPickerLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.apply {
                            preferences.clearImagePath()
                            preferences.saveImagePath(uri.toString())
                            paintView.updateBackgroundBitmap(bitmap)
                            preferences.saveBackgroundBitmap(bitmap)
                            preferences.clearImagePathOrigin()
                            preferences.saveImagePathOrigin(uri.toString())

                        }
                        Log.d("EditActivity", "Bitmap size: ${bitmap.width} x ${bitmap.height}")
                    } catch (e: Exception) {
                        Log.d("EditActivity", "Error loading image: ${e.message}")
                    }
                } else {
                    Log.d("EditActivity", "No image selected")
                }
            }
    }

    private fun launchPhoToPicker() {
        singlePhotoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    override fun onPause() {
        super.onPause()
        binding.paintView.saveTextItems(preferences)
    }

    override fun onResume() {
        super.onResume()
        binding.paintView.saveTextItems(preferences)
    }


}
