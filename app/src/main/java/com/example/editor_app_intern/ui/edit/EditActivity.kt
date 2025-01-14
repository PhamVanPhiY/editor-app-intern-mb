package com.example.editor_app_intern.ui.edit

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.example.editor_app_intern.adapter.FontAdapter
import com.example.editor_app_intern.constant.Constants.PATH_IMAGE_INTENT
import com.example.editor_app_intern.customeview.PaintView
import com.example.editor_app_intern.databinding.ActivityEditBinding
import com.example.editor_app_intern.model.FontItem
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.github.dhaval2404.colorpicker.model.ColorSwatch
import java.io.IOException

class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    private var isEraserEnabled = false
    private var isBackgroundRemoved = false
    private var textX: Float = 400f
    private var textY: Float = 600f
    var isDrawingEnabled = false
    private lateinit var editViewModel: EditViewModel
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var permissionsRequestLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var singlePhotoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var progressBarRemoveBackground: ProgressBar

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
        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val imagePath = intent.getStringExtra(PATH_IMAGE_INTENT)

        progressBarRemoveBackground = binding.progressBar
        imagePath?.let {
            val originalBitmap = BitmapFactory.decodeFile(it)
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

                        binding.paintView.setBackgroundBitmap(scaledBitmap)

                        Log.d(
                            "EditActivity",
                            "Scaled Bitmap size: ${scaledBitmap.width} x ${scaledBitmap.height}"
                        )
                        binding.paintView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            })
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//        setUpLauncher()
        setUpView()
        upLoadPhotoFromPhotoPicker()
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    @SuppressLint("ResourceType", "ClickableViewAccessibility")
    private fun setUpView() {
        binding.apply {
            btnDraw.setOnClickListener {
//                paintView.isDrawingEnabled = true
                paintView.startTouch(0f, 0f)
            }

            btnEraser.setOnClickListener {
                if (isEraserEnabled) {
                    paintView.disableEraser()
                    isEraserEnabled = false
                } else {
                    paintView.enableEraser()
                    isEraserEnabled = true
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
                    } else {
                        Toast.makeText(this@EditActivity, "Canvas is empty", Toast.LENGTH_SHORT)
                            .show()
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
                    paintView.drawText(textEntered, textX, textY)
                }
                paintView.isTextBoxVisible = true
            }



            btnSave.setOnClickListener {
                saveImage()
            }

//            btnAlbum.setOnClickListener {
//                if (hasStoragePermission()) {
//                    startActivity(Intent(this@EditActivity, AlbumActivity::class.java))
//                } else {
//                    permissionsRequestLauncher.launch(PERMISSIONS)
//                }
//            }

//            lbDraw.setOnClickListener {
//                movableEditText.visibility = View.VISIBLE
//                movableEditText.requestFocus()
//                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.showSoftInput(movableEditText, InputMethodManager.SHOW_IMPLICIT)
//
//                movableEditText.setOnEditorActionListener { v, actionId, event ->
//                    if (actionId == EditorInfo.IME_ACTION_DONE) {
//                        val userText = movableEditText.text.toString()
//                        paintView.updateTextFromEditText(userText) // Cập nhật văn bản cho PaintView
//                        movableEditText.visibility = View.GONE // Ẩn EditText sau khi nhập xong
//                        imm.hideSoftInputFromWindow(movableEditText.windowToken, 0) // Ẩn bàn phím
//                        true
//                    } else {
//                        false
//                    }
//                }
////            movableEditText.setOnTouchListener { v, event ->
////                when (event.action) {
////                    MotionEvent.ACTION_MOVE -> {
////                        val x = event.rawX - v.width / 2
////                        val y = event.rawY - v.height / 2
////
////                        val parent = paintView
////                        val maxX = parent.width - v.width
////                        val maxY = parent.height - v.height
////
////                        v.x = x.coerceIn(0f, maxX.toFloat())
////                        v.y = y.coerceIn(0f, maxY.toFloat())
////                    }
////                }
////                true
////            }
//
//            }
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
            paintView.setFont(font.fontPath) // Gọi phương thức setFont
        }
    }
//    private fun setUpLauncher() {
//        permissionsRequestLauncher = registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions()
//        ) { permissions ->
//            handlePermissionRequest(permissions)
//        }
//    }

//    private fun hasStoragePermission(): Boolean {
//        return atLeastVersionUpSideDownCake {
//            PERMISSIONS.any {
//                checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
//            }
//        } ?: PERMISSIONS.all {
//            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
//        }
//    }

    //    private fun handlePermissionRequest(permissions: Map<String, Boolean>) {
//        val granted = atLeastVersionUpSideDownCake {
//            permissions.values.any { it }
//        }?.let {
//            permissions.values.all { it }
//        } ?: false || hasStoragePermission()
//
//        if (granted)
//            startActivity(Intent(this@EditActivity, AlbumActivity::class.java))
//        else {
//            when {
//                PERMISSIONS.any {
//                    shouldShowRequestPermissionRationale(it)
//                } -> {
//                    NotificationDialog(
//                        title = getString(R.string.permission_required),
//                        message = getString(R.string.storage_permission_is_required_to_access_images),
//                        labelPositive = getString(R.string.ok),
//                        onPositive = {
//                            permissionsRequestLauncher.launch(PERMISSIONS)
//                        }
//                    ).show(supportFragmentManager, "PermissionDialog")
//                }
//
//                PERMISSIONS.any {
//                    !shouldShowRequestPermissionRationale(it)
//                } -> {
//                    OptionDialog(
//                        title = getString(R.string.permission_required),
//                        message = getString(R.string.permission_denied_permanently_dialog),
//                        labelPositive = getString(R.string.ok),
//                        labelNegative = getString(R.string.cancel),
//                        onPositive = {
//                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//                                data = Uri.fromParts("package", packageName, null)
//                            })
//                        },
//                    ).show(supportFragmentManager, "PermissionDialog")
//                }
//
//                else -> {
//                    permissionsRequestLauncher.launch(PERMISSIONS)
//                }
//            }
//        }
//    }
    fun clearText() {
        binding.apply {
            tvInputText.text.clear()
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

    fun openInputTextEdit(text: String, x: Float, y: Float) {
        binding.apply {
            val slideUpAnimation = AnimationUtils.loadAnimation(this@EditActivity, R.anim.slide_up)
            layoutInputText.visibility = ConstraintLayout.VISIBLE
            layoutInputText.startAnimation(slideUpAnimation)
            tvInputText.setText(text) // Hiển thị văn bản hiện tại
            tvInputText.isEnabled = true
            tvInputText.requestFocus()
            inputMethodManager.showSoftInput(tvInputText, InputMethodManager.SHOW_IMPLICIT)
            textX = x
            textY = y
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
        }
    }

    private fun saveImage() {
        val bitmap = binding.paintView.canvasBitmap
        val borderedBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, true)

//        borderedBitmap?.let { bmp ->
//            val canvas = Canvas(bmp)
//            binding.paintView.drawBorder(canvas)
//        }

        val imageName = "edited_image_${System.currentTimeMillis()}.jpg"
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, borderedBitmap?.width)
            put(MediaStore.Images.Media.HEIGHT, borderedBitmap?.height)
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH, resources.getString(R.string.path)
                )
            }
        }

        try {
            contentResolver.insert(imageCollection, contentValues)?.let { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        borderedBitmap?.compress(
                            Bitmap.CompressFormat.JPEG, 100, outputStream
                        )
                        Toast.makeText(
                            this@EditActivity, R.string.save_image_successfully, Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        throw IOException(resources.getString(R.string.exception_cant_open_output_stream))
                    }
                }
            }
                ?: throw IOException(resources.getString(R.string.exception_cant_create_record_in_media_store))
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                this@EditActivity,
                resources.getString(R.string.fail_to_save_image),
                Toast.LENGTH_SHORT
            ).show()
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
                            paintView.setBackgroundBitmap(bitmap)
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
}