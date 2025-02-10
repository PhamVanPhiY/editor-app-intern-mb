package com.example.editor_app_intern.ui.edit

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.editor_app_intern.InsetsWithKeyboardAnimationCallback
import com.example.editor_app_intern.InsetsWithKeyboardCallback
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
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val insetsWithKeyboardCallback = InsetsWithKeyboardCallback(window)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, insetsWithKeyboardCallback)
        ViewCompat.setWindowInsetsAnimationCallback(binding.root, insetsWithKeyboardCallback)
        editViewModel = ViewModelProvider(this).get(EditViewModel::class.java)

        editViewModel.fontList.observe(this, { fontList ->
            setUpFontRecyclerView(fontList)
        })
        val insetsWithKeyboardAnimationCallback =
            InsetsWithKeyboardAnimationCallback(binding.rcvFont)
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.rcvFont,
            insetsWithKeyboardAnimationCallback
        )

        gpuImage = GPUImage(this)
        hueFilter = GPUImageHueFilter(0f)
        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        preferences = SharedPreferences(this)
        binding.paintView.loadTextItems(preferences)
        val isEditAgain = intent.getBooleanExtra(IS_EDIT_AGAIN, false)
        if (isEditAgain) {
            loadImageEditAgain()
        }
        val imagePath = preferences.getImagePath()
        val backgroundBeforeStickerOpen = preferences.getBackgroundBitmap()
        if (backgroundBeforeStickerOpen != null) {
            binding.paintView.backgroundBitmap = backgroundBeforeStickerOpen
        }
        progressBarRemoveBackground = binding.progressBar
        imagePath?.let {
            var originalBitmap = BitmapFactory.decodeFile(it)
            binding.paintView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val paintViewWidth = binding.paintView.width
                    val paintViewHeight = binding.paintView.height
                    if (paintViewWidth > 0 && paintViewHeight > 0) {
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            originalBitmap!!,
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
        }

//        binding.paintView.viewTreeObserver.addOnGlobalLayoutListener(object :
//            ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                val paintViewWidth = binding.paintView.width
//                val paintViewHeight = binding.paintView.height
//
//                if (paintViewWidth > 0 && paintViewHeight > 0) {
//                    val originalBitmap =
//                        BitmapFactory.decodeResource(resources, R.drawable.filter_constrast)
//                    val scaleWidth = paintViewWidth.toFloat() / originalBitmap.width
//                    val scaleHeight = paintViewHeight.toFloat() / originalBitmap.height
//                    val scale = Math.min(scaleWidth, scaleHeight)
//
//                    val scaledWidth = (originalBitmap.width * scale).toInt()
//                    val scaledHeight = (originalBitmap.height * scale).toInt()
//                    val scaledBitmap =
//                        Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
//                    binding.paintView.updateBackgroundBitmap(scaledBitmap)
//
//                    binding.paintView.viewTreeObserver.removeOnGlobalLayoutListener(this)
//                }
//            }
//        })


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

                isCheckDraw = !isCheckDraw

                if (isCheckDraw) {
                    paintView.isDrawingEnabled = true
                    binding.lineDraw.visibility = View.VISIBLE
                } else {
                    paintView.isDrawingEnabled = false
                    binding.lineDraw.visibility = View.INVISIBLE
                }
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
                setBrushColor(this@EditActivity, paintView, btnColorPicked)
            }

            btnColorPicked.setOnClickListener {
                setBrushColor(this@EditActivity, paintView, btnColorPicked)
            }

            btnColor.setOnClickListener {
                setBrushColor(this@EditActivity, paintView, btnColorPicked)
            }

            btnSelectImage.setOnClickListener {
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
                CoroutineScope(Dispatchers.Main).launch {
                    paintView.isTextBoxVisible = false
                    paintView.isStickerTextBoxVisible = false
                    paintView.selectedTextItem = null
                    paintView.selectedStickerItem = null

                    val saveBitmapDeferred = async(Dispatchers.IO) {
                        paintView.backgroundBitmap?.let { bitmap ->
                            preferences.saveBackgroundBitmap(bitmap)
                        }
                    }
                    saveBitmapDeferred.await()

                    paintView.invalidate()

                    val saveImageDeferred = async(Dispatchers.IO) {
                        saveImage().toString()
                    }
                    savedImageURI = saveImageDeferred.await()

                    val intent = Intent(this@EditActivity, ResultActivity::class.java).apply {
                        putExtra(PATH_IMAGE_JUST_SAVED, savedImageURI)
                    }
                    startActivity(intent)
                    finish()
                }
            }

            btnHue.setOnClickListener {
                openEditHue()
            }

            btnSticker.setOnClickListener {
                startActivity(Intent(this@EditActivity, StickerActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()

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

    private fun applyHueFilter() {

        if (binding.paintView.backgroundBitmap != null) {
            CoroutineScope(Dispatchers.Default).launch {
                gpuImage.setImage(binding.paintView.backgroundBitmap)
                gpuImage.setFilter(hueFilter)
                val filteredBitmap = gpuImage.bitmapWithFilterApplied

                withContext(Dispatchers.Main) {
                    if (filteredBitmap != null && filteredBitmap.width > 0 && filteredBitmap.height > 0) {
                        binding.paintView.backgroundBitmap = filteredBitmap
                        binding.paintView.invalidate()
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
            val slideUpAnimation = AnimationUtils.loadAnimation(this@EditActivity, R.anim.slide_up)
            layoutInputText.visibility = ConstraintLayout.VISIBLE
            layoutInputText.startAnimation(slideUpAnimation)
            tvInputText.isEnabled = true
            tvInputText.requestFocus()
            inputMethodManager.showSoftInput(tvInputText, InputMethodManager.SHOW_IMPLICIT)
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

    private fun saveImage(): Uri? {
        binding.apply {
            val bitmap = paintView.canvasBitmap
            val imageName = "edited_image_${System.currentTimeMillis()}.jpg"
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.WIDTH, bitmap?.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap?.height)
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, resources.getString(R.string.path))
                }
            }

            return try {
                contentResolver.insert(imageCollection, contentValues)?.let { uri ->
                    contentResolver.openOutputStream(uri).use { outputStream ->
                        if (outputStream != null) {
                            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(
                                    this@EditActivity,
                                    R.string.save_image_successfully,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            uri
                        } else {
                            throw IOException(resources.getString(R.string.exception_cant_open_output_stream))
                        }
                    }
                }
                    ?: throw IOException(resources.getString(R.string.exception_cant_create_record_in_media_store))
            } catch (e: IOException) {
                e.printStackTrace()

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        this@EditActivity,
                        resources.getString(R.string.fail_to_save_image),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                null
            }
        }
    }


    @SuppressLint("ResourceType")
    private fun setBrushColor(context: Context, paintView: PaintView, btnColorPicked: ImageView) {
        MaterialColorPickerDialog.Builder(context).setTitle(R.string.pick_color)
            .setColorShape(ColorShape.CIRCLE).setColorSwatch(ColorSwatch._300)
            .setDefaultColor(Color.BLACK).setColorListener { color, colorHex ->
                paintView.setBrushColor(color);
                val drawable = btnColorPicked.background.mutate()
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
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
                            paintView.clearCanvas()
                            preferences.clearImagePath()
                            paintView.updateBackgroundBitmap(bitmap)
                            preferences.saveImagePath(uri.toString())

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

//    override fun onStop() {
//        preferences.clearTextItems()
//        super.onStop()
//
//    }

    override fun onDestroy() {
        preferences.clearImagePath()
//        preferences.clearStickers()
//        preferences.clearTextItems()
        super.onDestroy()
    }
}