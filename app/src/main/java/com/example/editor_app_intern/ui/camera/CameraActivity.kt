package com.example.editor_app_intern.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.editor_app_intern.R
import com.example.editor_app_intern.SharedPreferences
import com.example.editor_app_intern.adapter.FilterCameraAdapter
import com.example.editor_app_intern.constant.Constants
import com.example.editor_app_intern.constant.Constants.PATH_IMAGE_INTENT
import com.example.editor_app_intern.databinding.ActivityCameraBinding
import com.example.editor_app_intern.model.FilterCamera
import com.example.editor_app_intern.ui.edit.EditActivity
import com.example.editor_app_intern.utils.YuvToRgbConverter
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageCrosshatchFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGammaFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGlassSphereFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImagePixelationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSketchFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSolarizeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var loadingImageView: ImageView
    private lateinit var preferences: SharedPreferences
    private var countdownTimer: CountDownTimer? = null
    private var countdownTimeInMillis: Long = 0
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraViewModel: CameraViewModel
    private var imageCapture: ImageCapture? = null
    private lateinit var converter: YuvToRgbConverter
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var gpuImageView: GPUImageView
    private var isFrontCamera = false
    private var bitmap: Bitmap? = null
    private var isFilterVisible = false
    private var isTimeVisible = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCameraBinding.inflate(layoutInflater)
        cameraViewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
        setContentView(binding.root)
        preferences = SharedPreferences(this)
        countdownTimeInMillis = preferences.getTimerValue()
        if (countdownTimeInMillis > 0) {
            binding.timerHeader.visibility = View.VISIBLE
            val timerSecond = countdownTimeInMillis / 1000
            binding.tvTimer.text = timerSecond.toString()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        cameraViewModel.filters.observe(this, { filterList ->
            setUpFilterRecyclerView(filterList)
        })

        setUpView()
    }

    private fun setUpFilterRecyclerView(filterList: List<FilterCamera>) {
        val filterAdapter = FilterCameraAdapter(filterList) { filterCamera ->
            applyFilter(filterCamera)
        }
        binding.apply {
            rcvFilterCamera.layoutManager =
                LinearLayoutManager(this@CameraActivity, RecyclerView.HORIZONTAL, false)
            rcvFilterCamera.adapter = filterAdapter
        }
    }

    private fun applyFilter(filterCamera: FilterCamera) {
        gpuImageView.filter = when (filterCamera.nameFilter) {
            Constants.FILTER_NORMAL -> GPUImageFilter()
            Constants.FILTER_SKETCH -> GPUImageSketchFilter()
            Constants.FILTER_INVERT -> GPUImageColorInvertFilter()
            Constants.FILTER_SOLARIZE -> GPUImageSolarizeFilter()
            Constants.FILTER_GRAY_SCALE -> GPUImageGrayscaleFilter()
            Constants.FILTER_BRIGHTNESS -> GPUImageBrightnessFilter(.3f)
            Constants.FILTER_CONTRAST -> GPUImageContrastFilter(2f)
            Constants.FILTER_PIXELATION -> GPUImagePixelationFilter().apply { setPixel(20F) }
            Constants.FILTER_GLASS -> GPUImageGlassSphereFilter()
            Constants.FILTER_CROSS_HATCH -> GPUImageCrosshatchFilter()
            Constants.FILTER_GAMMA -> GPUImageGammaFilter(2f)
            else -> null
        }
        gpuImageView.requestRender()
    }

    private fun setUpView() {
        supportActionBar?.hide()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.apply {
            cameraCaptureButton.setOnClickListener {
                takePhoto()
            }
            converter = YuvToRgbConverter(this@CameraActivity)
            this@CameraActivity.gpuImageView = binding.gpuImageView
            gpuImageView.rotation = 90F
            gpuImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
            outputDirectory = getOutputDirectory()
            cameraExecutor = Executors.newSingleThreadExecutor()

            btnFilter.setOnClickListener {
                toggleFilterVisibility()
            }

            btnSelectCam.setOnClickListener {
                isFrontCamera = !isFrontCamera
                startCamera()
            }
            val layoutParams = gpuImageView.layoutParams
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.size_600dp)
            layoutParams.height = resources.getDimensionPixelSize(R.dimen.size_600dp)
            gpuImageView.layoutParams = layoutParams

            btnTime.setOnClickListener {
                toggleTimeVisibility()
                tvTime3s.setOnClickListener {
                    enableTimer(3)
                }
                tvTime5s.setOnClickListener {
                    enableTimer(5)
                }
                tvTime7s.setOnClickListener {
                    enableTimer(7)
                }
                tvTime10s.setOnClickListener {
                    enableTimer(10)
                }
                btnTurnOffTimer.setOnClickListener {
                    binding.timerHeader.visibility = View.INVISIBLE
                    countdownTimeInMillis = 0
                    countdownTimer?.cancel()
                    preferences.clearTimerValue()
                    toggleTimeVisibility()
                }
            }

        }

    }

    @SuppressLint("SetTextI18n")
    private fun enableTimer(timer: Int) {
        countdownTimeInMillis = timer * 1000L
        preferences.saveTimerValue(countdownTimeInMillis)
        binding.apply {
            timerHeader.visibility = View.VISIBLE
            tvTimer.text = timer.toString()
            countDownTimer.text = timer.toString()
        }
        toggleTimeVisibility()
    }

    private fun toggleTimeVisibility() {
        isTimeVisible = !isTimeVisible
        binding.layoutTime.visibility = if (isTimeVisible) View.VISIBLE else View.GONE
    }

    private fun toggleFilterVisibility() {
        isFilterVisible = !isFilterVisible
        binding.rcvFilterCamera.visibility = if (isFilterVisible) View.VISIBLE else View.INVISIBLE
    }

    private fun takePhoto() {
        if (countdownTimeInMillis > 0) {
            binding.countDownTimer.visibility = View.VISIBLE
            startCountdown()
        } else {
            captureImage()
        }
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(
                        "countdownTimeInMillis 1112233",
                        "Photo capture failed: ${exc.message}",
                        exc
                    )
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val filteredBitmap = gpuImageView.gpuImage.bitmapWithFilterApplied
                        saveFilteredBitmapToFile(filteredBitmap, photoFile)
                        notifyImageSaved(photoFile.absolutePath)
                    }
                }
            }
        )
    }


    private fun startCountdown() {
        countdownTimer?.cancel()

        countdownTimer = object : CountDownTimer(countdownTimeInMillis, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                countdownTimeInMillis = millisUntilFinished
                binding.countDownTimer.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                captureImage()
                binding.countDownTimer.visibility = View.GONE
                countdownTimeInMillis = 0
            }
        }.start()

        binding.countDownTimer.visibility = View.VISIBLE
    }

    private suspend fun saveFilteredBitmapToFile(bitmap: Bitmap?, file: File) {
        if (bitmap != null) {
            try {
                val finalBitmap = if (isFrontCamera) {
                    bitmap
                } else {
                    bitmap
                }
                val rotatedBitmap = rotateBitmap(finalBitmap, 90f)
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { outputStream ->
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                }
                Log.d(TAG, "Filtered bitmap saved successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving filtered bitmap: ${e.message}")
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private suspend fun notifyImageSaved(imagePath: String) {
        withContext(Dispatchers.Main) {
            val intent = Intent(this@CameraActivity, EditActivity::class.java).apply {
                putExtra(PATH_IMAGE_INTENT, imagePath)
                preferences.saveImagePath(imagePath)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {

            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            imageCapture = ImageCapture.Builder().build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                processImage(imageProxy)
            })

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        CoroutineScope(Dispatchers.Default).launch {
            val bitmap = allocateBitmapIfNecessary(imageProxy.width, imageProxy.height)
            converter.yuvToRgb(imageProxy.image!!, bitmap)
            val processedBitmap = if (isFrontCamera) {
                createMirroredBitmap(bitmap)
            } else {
                bitmap
            }
            withContext(Dispatchers.Main) {
                gpuImageView.setImage(processedBitmap)
            }
            imageProxy.close()
        }
    }

    private fun allocateBitmapIfNecessary(width: Int, height: Int): Bitmap {
        if (bitmap == null || bitmap!!.width != width || bitmap!!.height != height) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        return bitmap!!
    }

    companion object {
        private const val TAG = "CameraXGFG"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private fun createMirroredBitmap(originalBitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )
    }

    override fun onDestroy() {
        // preferences.clearTimerValue()
        countdownTimer?.cancel()
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

