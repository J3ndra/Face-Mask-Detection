package com.junianto.facemaskdetection.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.junianto.facemaskdetection.R
import com.junianto.facemaskdetection.base.BaseActivity
import com.junianto.facemaskdetection.base.NoViewModelActivity
import com.junianto.facemaskdetection.data.Recognition
import com.junianto.facemaskdetection.ml.ModelMaskImageClassification
import com.junianto.facemaskdetection.ui.camera.recoginition.RecognitionViewModel
import com.junianto.facemaskdetection.ui.camera.recoginition.RecognitionAdapter
import com.junianto.facemaskdetection.util.Cons
import com.junianto.facemaskdetection.util.Cons.Companion.MAX_RESULT_DISPLAY
import com.junianto.facemaskdetection.util.Cons.Companion.REQUEST_CODE_PERMISSIONS
import com.junianto.facemaskdetection.util.Cons.Companion.REQUIRED_PERMISSIONS
import com.junianto.facemaskdetection.util.Cons.Companion.TAG
import com.junianto.facemaskdetection.util.YuvToRgbConverter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.model.Model
import java.util.concurrent.Executors

typealias RecognitionListener = (recognition: List<Recognition>) -> Unit

class CameraActivity : BaseActivity<RecognitionViewModel>() {

    // CameraX variables
    private lateinit var preview: Preview // Preview use case, fast, responsive view of the camera
    private lateinit var imageAnalyzer: ImageAnalysis // Analysis use case, for running ML code
    private lateinit var camera: Camera
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraSelector: CameraSelector? = null

    // Views attachment
    private val resultRecyclerView by lazy {
        findViewById<RecyclerView>(R.id.recognitionResults)
    }
    private val viewFinder by lazy {
        findViewById<PreviewView>(R.id.viewFinder)
    }
    private val cameraSwitchBtn by lazy {
        findViewById<ImageView>(R.id.camera_switch_button)
    }

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val recognitionViewModel: RecognitionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_main)

        // Requesting Camera Permission
        if (allPermissionGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Initialising the resultRecyclerView and its linked viewAdapter
        val viewAdapter = RecognitionAdapter(this)
        resultRecyclerView.adapter = viewAdapter

        resultRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        /** Disable recycler view animation to reduce flickering, otherwise items can move, fade in
         *  and out as the list change */
        resultRecyclerView.itemAnimator = null

        /** Attach an observer on the LiveData field of recognitionList
         *  This will notify the recycler view to update every time when a new list is set on the
         *  LiveData field of recognitionList. */
        recognitionViewModel.recognitionList.observe(this, {
            viewAdapter.submitList(it)
        })
    }

    /**
     * Check all permissions are granted - use for Camera permission in this example.
     */
    private fun allPermissionGranted(): Boolean = Cons.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions's denied!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(224, 224))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysisUseCase: ImageAnalysis ->
                    analysisUseCase.setAnalyzer(cameraExecutor, ImageAnalyzer(this) { items ->
                        // updating the list of recognised objects
                        recognitionViewModel.updateData(items)
                    })
                }

            if (cameraSelector == null) {
                // Select camera, back is the default. If it is not available, choose front camera
                cameraSelector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    else -> null
                }
                if (cameraSelector == null) finish()
            }

            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                // Add switch if front camera available
                cameraSwitchBtn.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                        startCamera()
                    }
                }
            }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                /** Bind use cases to camera - try to bind everything at once and CameraX will find
                 * the best combination.
                 */
                cameraSelector?.let {
                    camera = cameraProvider.bindToLifecycle(this, it, preview, imageAnalyzer)
                }

                // Attach the preview to preview view, aka View Finder
                preview.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed.", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private class ImageAnalyzer(context: Context, private val listener: RecognitionListener): ImageAnalysis.Analyzer {

        private val maskModel: ModelMaskImageClassification by lazy {

            // GPU Acceleration
            val compatList = CompatibilityList()

            val options = if (compatList.isDelegateSupportedOnThisDevice) {
                Log.d(TAG, "This device is GPU Compatible.")
                Model.Options.Builder().setDevice(Model.Device.GPU).build()
            } else {
                Log.d(TAG, "This device is GPU Incompatible")
                Model.Options.Builder().setNumThreads(4).build()
            }

            // Initialize the Mask Model
            ModelMaskImageClassification.newInstance(context, options)
        }

        override fun analyze(imageProxy: ImageProxy) {

            val items = mutableListOf<Recognition>()

            val tfImage = TensorImage.fromBitmap(toBitmap(imageProxy))

            val outputs = maskModel.process(tfImage)
                .probabilityAsCategoryList.apply {
                    sortByDescending { it.score }
                }.take(MAX_RESULT_DISPLAY)

            for (output in outputs) {
                items.add(Recognition(output.label, output.score))
            }

            // Return the result
            listener(items.toList())

            // Close the image
            imageProxy.close()
        }

        /**
         * Convert Image Proxy to Bitmap
         */
        private val yuvToRgbConverter = YuvToRgbConverter(context)
        private lateinit var bitmapBuffer: Bitmap
        private lateinit var rotationMatrix: Matrix

        @SuppressLint("UnsafeExperimentalUsageError")
        private fun toBitmap(imageProxy: ImageProxy): Bitmap? {

            val image = imageProxy.image ?: return null

            // Initialise Buffer
            if (!::bitmapBuffer.isInitialized) {
                // The image rotation and RGB image buffer are initialized only once
                Log.d(TAG, "Initialise toBitmap()")
                rotationMatrix = Matrix()
                rotationMatrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            }

            // Pass image to an image analyzer
            yuvToRgbConverter.yuvToRgb(image, bitmapBuffer)

            // Create the Bitmap in the correct orientation
            return Bitmap.createBitmap(
                bitmapBuffer,
                0,
                0,
                bitmapBuffer.width,
                bitmapBuffer.height,
                rotationMatrix,
                false
            )
        }
    }
}