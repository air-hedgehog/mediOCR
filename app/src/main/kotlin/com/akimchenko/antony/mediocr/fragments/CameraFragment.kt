package com.akimchenko.antony.mediocr.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.util.Pair
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.Utils
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.*
import java.util.*
import kotlin.math.absoluteValue


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CameraFragment : Fragment(), SensorEventListener {

    companion object {
        const val SENSOR_THRESHOLD = 2.0f
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }

    private var currentRotation = Surface.ROTATION_0

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSessions: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            //open your shutter here
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Transform you image captured size according to the surface width and height
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            //This is called when the shutter is open
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private val pictureWidthHeight: Pair<Int, Int>?
        get() {
            cameraDevice ?: return null
            val activity = activity as MainActivity? ?: return null
            val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager? ?: return null
            try {
                val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
                val jpegSizes: Array<Size>? =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(
                        ImageFormat.JPEG
                    )
                if (jpegSizes != null && jpegSizes.isNotEmpty())
                    return Pair(jpegSizes[0].width, jpegSizes[0].height)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = activity as MainActivity? ?: return
        sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity?
        activity ?: return
        texture_view.surfaceTextureListener = textureListener
        capture_button.setImageDrawable(
            Utils.makeSelector(
                activity,
                ContextCompat.getDrawable(
                    activity,
                    R.drawable.capture_button
                )!!.toBitmap()
            )
        )
        capture_button.setOnClickListener { takePicture() }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun takePicture() {
        val activity = activity as MainActivity? ?: return
        cameraDevice ?: return
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager? ?: return
        try {
            val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
            val jpegSizes =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.JPEG)
            var width = 640
            var height = 480
            if (jpegSizes != null && jpegSizes.isNotEmpty()) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }
            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurfaces = ArrayList<Surface>(2)
            outputSurfaces.add(reader.surface)
            outputSurfaces.add(Surface(texture_view.surfaceTexture))
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(currentRotation))
            val defaultDirectory =
                File("${Environment.getExternalStorageDirectory()}/${activity.getString(R.string.default_folder_name)}")
            if (!defaultDirectory.exists() || !defaultDirectory.isDirectory)
                defaultDirectory.mkdir()

            val file = File("$defaultDirectory/${Calendar.getInstance().timeInMillis}.jpg")
            val readerListener = object : ImageReader.OnImageAvailableListener {
                override fun onImageAvailable(reader: ImageReader) {
                    var image: Image? = null
                    try {
                        image = reader.acquireLatestImage()
                        val buffer = image!!.planes[0].buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)
                        save(bytes)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        image?.close()
                    }
                }

                @Throws(IOException::class)
                private fun save(bytes: ByteArray) {
                    var output: OutputStream? = null
                    try {
                        output = FileOutputStream(file)
                        output.write(bytes)
                    } finally {
                        output?.close()
                    }
                }
            }
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
            val captureListener = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    val previewFragment = PreviewFragment()
                    val args = Bundle()
                    args.putString(PreviewFragment.ARG_IMAGE_FILE, file.path)
                    previewFragment.arguments = args
                    activity.pushFragment(previewFragment)
                    //Toast.makeText(activity, "${activity.getString(R.string.saved)}: $file", Toast.LENGTH_SHORT).show()
                    //createCameraPreview()
                }
            }
            cameraDevice!!.createCaptureSession(outputSurfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }

                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun createCameraPreview() {
        cameraDevice ?: return
        texture_view ?: return
        try {
            val texture = texture_view.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(surface)
            cameraDevice!!.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    //The shutter is already closed
                    if (null == cameraDevice) {
                        return
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession
                    val widthHeight = pictureWidthHeight
                    if (widthHeight != null)
                        texture_view.setAspectRatio(widthHeight.second, widthHeight.first)
                    updatePreview()
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    val activity = activity as MainActivity? ?: return
                    Toast.makeText(activity, "Configuration change", Toast.LENGTH_SHORT).show()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val activity = activity as MainActivity? ?: return
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun updatePreview() {
        captureRequestBuilder ?: return
        captureRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions!!.setRepeatingRequest(captureRequestBuilder!!.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        if (cameraDevice != null) {
            cameraDevice!!.close()
            cameraDevice = null
        }
        if (imageReader != null) {
            imageReader!!.close()
            imageReader = null
        }
    }

    override fun onResume() {
        super.onResume()
        val activity = activity as MainActivity? ?: return
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        startBackgroundThread()
        if (texture_view.isAvailable) {
            openCamera()
        } else {
            texture_view.surfaceTextureListener = textureListener
        }
    }

    override fun onPause() {
        val activity = activity as MainActivity? ?: return
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        sensorManager!!.unregisterListener(this)
        stopBackgroundThread()
        closeCamera()
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val sensorX = event.values[0]
        val sensorY = event.values[1]
        onNewSensorValues(sensorX, sensorY)
    }

    private fun onNewSensorValues(x: Float, y: Float) {
        if (y.absoluteValue < SENSOR_THRESHOLD && 10 - x.absoluteValue < SENSOR_THRESHOLD) {
            //landscape
            if (x > 0)
                setCurrentRotation(Surface.ROTATION_90)
            else
                setCurrentRotation(Surface.ROTATION_270)
        }
        if (x.absoluteValue < SENSOR_THRESHOLD && 10 - y.absoluteValue < SENSOR_THRESHOLD) {
            //portrait
            if (y > 0)
                setCurrentRotation(Surface.ROTATION_0)
            else
                setCurrentRotation(Surface.ROTATION_180)
        }
    }

    private fun setCurrentRotation(rotation: Int) {
        if (currentRotation != rotation)
            currentRotation = rotation
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }
}
