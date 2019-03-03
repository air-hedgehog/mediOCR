package com.akimchenko.antony.mediocr

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.view.Surface
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment

import java.util.Arrays

class SomeClass : Fragment() {


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected fun createCameraPreview() {
        try {
            val texture = textureView.getSurfaceTexture()!!
            texture!!.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight())
            val surface = Surface(texture)
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)
            cameraDevice.createCaptureSession(Arrays.asList(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession
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
}
