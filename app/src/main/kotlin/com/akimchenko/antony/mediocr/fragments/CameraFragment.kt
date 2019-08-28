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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Pair
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.Utils
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.math.absoluteValue


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CameraFragment(override val layoutResId: Int = R.layout.fragment_camera) : BaseFragment(), SensorEventListener {

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
    private var flashMode: Int = CameraMetadata.FLASH_MODE_OFF
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
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) = openCamera()

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false

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
            val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
                ?: return null
            try {
                val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
                val jpegSizes: Array<Size>? =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(
                        ImageFormat.JPEG
                    )
                if (jpegSizes != null && jpegSizes.isNotEmpty())
                    return Pair(jpegSizes[0].width, jpegSizes[0].height)
            } catch (e: CameraAccessException) {
                e.message?.let { Log.e(CameraFragment::class.java.name, it) }

            }

            return null
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity?
        activity ?: return
        texture_view.surfaceTextureListener = textureListener
        (capture_button as ImageView).setImageDrawable(
            Utils.makeSelector(
                activity,
                ContextCompat.getDrawable(
                    activity,
                    R.drawable.capture_button
                )!!.toBitmap()
            )
        )
        (capture_button as ImageView).setOnClickListener { takePicture() }

        (flash_button as ImageView).setImageDrawable(
            ContextCompat.getDrawable(
                activity,
                when (flashMode) {
                    CameraMetadata.FLASH_MODE_OFF -> R.drawable.flash_off
                    CameraMetadata.FLASH_MODE_TORCH -> R.drawable.flash
                    else -> R.drawable.flash_off
                }
            )
        )
        (flash_button as ImageView).setOnClickListener { setFlashMode() }
    }

    private fun setFlashMode() {
        val activity = activity as MainActivity? ?: return
        var drawableId: Int? = null
        when (flashMode) {
            CameraMetadata.FLASH_MODE_OFF -> {
                flashMode = CameraMetadata.FLASH_MODE_TORCH
                drawableId = R.drawable.flash
            }
            CameraMetadata.FLASH_MODE_TORCH -> {
                flashMode = CameraMetadata.FLASH_MODE_OFF
                drawableId = R.drawable.flash_off
            }
        }
        if (drawableId != null)
            (flash_button as ImageView).setImageDrawable(
                Utils.makeSelector(
                    activity,
                    ContextCompat.getDrawable(activity, drawableId)!!.toBitmap()
                )
            )
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
            e.message?.let { Log.e(CameraFragment::class.java.name, it) }
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
            captureBuilder.set(CaptureRequest.FLASH_MODE, flashMode)
            val file = activity.getFileForBitmap()
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
                        e.message?.let { Log.e(CameraFragment::class.java.name, it) }
                    } catch (e: IOException) {
                        e.message?.let { Log.e(CameraFragment::class.java.name, it) }
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
                    activity.pushFragment(PreviewFragment().also {
                        it.arguments =
                            Bundle().also { args ->
                                args.putString(
                                    PreviewFragment.ARG_IMAGE_FILE_URI,
                                    file.toUri().toString()
                                )
                            }
                    })
                }
            }
            cameraDevice!!.createCaptureSession(outputSurfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler)
                    } catch (e: CameraAccessException) {
                        e.message?.let { Log.e(CameraFragment::class.java.name, it) }
                    }

                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.message?.let { Log.e(CameraFragment::class.java.name, it) }
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
                    Log.e(CameraFragment::class.java.name, "configuration failed")
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.message?.let { Log.e(CameraFragment::class.java.name, it) }
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
            e.message?.let { Log.e(CameraFragment::class.java.name, it) }
        }
    }

    private fun updatePreview() {
        captureRequestBuilder ?: return
        captureRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions!!.setRepeatingRequest(captureRequestBuilder!!.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.message?.let { Log.e(CameraFragment::class.java.name, it) }
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
        sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager? ?: return
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        startBackgroundThread()
        if (texture_view.isAvailable)
            openCamera()
        else
            texture_view.surfaceTextureListener = textureListener
    }

    override fun onPause() {
        super.onPause()
        val activity = activity as MainActivity? ?: return
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        sensorManager?.unregisterListener(this)
        stopBackgroundThread()
        closeCamera()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val sensorX = event.values[0]
        val sensorY = event.values[1]
        onNewSensorValues(sensorX, sensorY)
    }

    private fun onNewSensorValues(x: Float, y: Float) {
        if (y.absoluteValue < 10 - SENSOR_THRESHOLD && x.absoluteValue < 10 - SENSOR_THRESHOLD) return
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
