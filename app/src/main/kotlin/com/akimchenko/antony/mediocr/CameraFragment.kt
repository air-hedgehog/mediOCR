package com.akimchenko.antony.mediocr

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.*
import java.util.*


@SuppressLint("NewApi")
class CameraFragment : Fragment(), View.OnClickListener, TextureView.SurfaceTextureListener {

    companion object {
        const val READ_WRITE_CAMERA_REQUEST_CODE = 101
    }

    private val orientations: SparseIntArray = SparseIntArray()
    private var cameraId: String = ""
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSessions: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            //This is called when the camera is open
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            onBackFragmentPressed()
        }
    }

    private fun onBackFragmentPressed() {
        closeCamera()
        activity?.onBackPressed()
    }

    init {
        orientations.append(Surface.ROTATION_0, 90)
        orientations.append(Surface.ROTATION_90, 0)
        orientations.append(Surface.ROTATION_180, 270)
        orientations.append(Surface.ROTATION_270, 180)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        texture_view.surfaceTextureListener = this
        cancel_button.setOnClickListener(this)
        accept_button.setOnClickListener(this)
        capture_button.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (texture_view.isAvailable)
            openCamera()
        else
            texture_view.surfaceTextureListener = this
    }

    override fun onPause() {
        super.onPause()
        closeCamera()
        stopBackgroundThread()
    }

    override fun onClick(v: View?) {
        when (v) {
            cancel_button -> onBackFragmentPressed()
            accept_button -> {//TODO
            }
            capture_button -> takePicture()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        // Transform you image captured size according to the surface width and height
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean = false

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        openCamera()
    }

    private fun getPictureWidthHeight(): Pair<Int, Int>? {
        val manager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
        val characteristics = manager?.getCameraCharacteristics(cameraDevice!!.id)
        val jpegSizes: Array<Size>? =
                characteristics?.get<StreamConfigurationMap>(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                        .getOutputSizes(ImageFormat.JPEG)
        return if (jpegSizes == null || jpegSizes.isEmpty()) null else Pair(jpegSizes[0].width, jpegSizes[0].height)
    }

    private fun takePicture() {
        val activity: MainActivity? = activity as MainActivity?
        activity ?: return
        cameraDevice ?: return
        try {
            var width = 640
            var height = 480
            val widthHeight = getPictureWidthHeight()
            if (widthHeight != null) {
                width = widthHeight.first
                height = widthHeight.second
            }
            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurfaces = ArrayList<Surface>(2)
            outputSurfaces.add(reader.surface)
            outputSurfaces.add(Surface(texture_view.surfaceTexture))
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            // Orientation
            val rotation: Int? = activity.windowManager?.defaultDisplay?.rotation
            rotation ?: return
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientations.get(rotation))

            val defaultDirectory = File("${Environment.getExternalStorageDirectory()}/${activity.getString(R.string.default_folder_name)}")
            if (!defaultDirectory.exists() || !defaultDirectory.isDirectory)
                defaultDirectory.mkdir()

            val file = File("$defaultDirectory/${Calendar.getInstance().timeInMillis}.jpg")
            file.createNewFile()

            val readerListener: ImageReader.OnImageAvailableListener = object : ImageReader.OnImageAvailableListener {
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
                    Toast.makeText(activity, "Saved:$file", Toast.LENGTH_SHORT).show()
                    createCameraPreview()
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

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread?.start()
        mBackgroundHandler = Handler(mBackgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    private fun createCameraPreview() {
        try {
            val texture = texture_view.surfaceTexture!!
            imageDimension ?: return
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface)
            cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    //The camera is already closed
                    cameraDevice ?: return
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession


                    val widthHeight = getPictureWidthHeight()
                    if (widthHeight != null)
                        texture_view.setAspectRatio(widthHeight.second, widthHeight.first)
                    updatePreview()
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Toast.makeText(activity, "Configuration change", Toast.LENGTH_SHORT).show()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val activity: MainActivity? = activity as MainActivity?
        activity ?: return
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
        try {
            cameraId = manager!!.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]

            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }


    }

    private fun updatePreview() {
        cameraDevice ?: return
        captureRequestBuilder ?: return
        captureRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

       /* val rotation = activity?.windowManager?.defaultDisplay?.rotation
        rotation ?: return
        texture_view.rotation = when (rotation) {
            Surface.ROTATION_90 -> -90.0f
            Surface.ROTATION_0 -> 0.0f
            Surface.ROTATION_180 -> -180.0f
            Surface.ROTATION_270 -> -270.0f
            else -> 0.0f
        }*/
        try {
            cameraCaptureSessions?.setRepeatingRequest(captureRequestBuilder!!.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updatePreview()
    }

    private fun closeCamera() {
        cameraDevice?.close()
        cameraDevice = null

        imageReader?.close()
        imageReader = null
    }

}