package com.akimchenko.antony.mediocr.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.adapters.MainFragmentAdapter
import com.akimchenko.antony.mediocr.utils.Utils
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File

class MainFragment : Fragment(), View.OnClickListener {

    private var newPhotoFile: File? = null

    companion object {
        const val READ_WRITE_CAMERA_REQUEST_CODE = 101
        const val CAPTURE_IMAGE_REQUEST_CODE = 102
        const val GALLERY_CHOOSER_REQUEST_CODE = 103
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity: MainActivity? = activity as MainActivity?
        activity ?: return
        recycler_view.layoutManager = GridLayoutManager(activity, 2)
        recycler_view.adapter = MainFragmentAdapter(activity, ArrayList<File>().also { it.addAll(activity.getDefaultSavedFilesDirectory().listFiles()) })
        camera_button.setImageDrawable(
            Utils.makeSelector(
                activity,
                ContextCompat.getDrawable(activity, R.drawable.camera_button)!!.toBitmap()
            )
        )
        gallery_button.setImageDrawable(
            Utils.makeSelector(
                activity,
                ContextCompat.getDrawable(
                    activity,
                    R.drawable.gallery_button
                )!!.toBitmap()
            )
        )
        camera_button.setOnClickListener(this)
        gallery_button.setOnClickListener(this)
    }

    @SuppressLint("InlinedApi")
    override fun onClick(v: View?) {
        val activity: MainActivity? = activity as MainActivity?
        activity ?: return
        when (v) {
            camera_button -> {
                activity.requestPermissions(arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ),
                    READ_WRITE_CAMERA_REQUEST_CODE,
                    object : MainActivity.OnRequestPermissionCallback {
                        override fun onPermissionReturned(isGranted: Boolean) {
                            if (isGranted)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                    activity.pushFragment(CameraFragment())
                                else
                                    sendCameraIntent()
                            else
                                Toast.makeText(
                                    activity,
                                    activity.getString(R.string.you_need_to_allow_permissions_read_write_camera),
                                    Toast.LENGTH_LONG
                                ).show()
                        }
                    })
            }
            gallery_button -> sendGalleryChooserIntent()

        }
    }

    private fun sendCameraIntent() {
        val activity = activity as MainActivity? ?: return
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(activity.packageManager)?.also {
                newPhotoFile = activity.getFileForBitmap()
                if (!newPhotoFile!!.exists())
                    newPhotoFile!!.createNewFile()
                newPhotoFile!!.also {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, newPhotoFile!!.toUri())
                    startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST_CODE)
                }
            }
        }
    }

    private fun sendGalleryChooserIntent() {
        Intent().also {
            it.type = "image/*"
            it.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(it, null), GALLERY_CHOOSER_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val activity = activity as MainActivity? ?: return
        when (requestCode) {
            CAPTURE_IMAGE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && newPhotoFile != null) {
                    activity.pushFragment(PreviewFragment().also {
                        it.arguments = Bundle().also { args ->
                            args.putString(
                                PreviewFragment.ARG_IMAGE_FILE_URI,
                                newPhotoFile!!.toUri().toString()
                            )
                        }
                    })
                }
            }
            GALLERY_CHOOSER_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val uri = data?.data ?: return
                    activity.pushFragment(PreviewFragment().also {
                        it.arguments = Bundle().also { args ->
                            args.putString(
                                PreviewFragment.ARG_IMAGE_FILE_URI,
                                uri.toString()
                            )
                        }
                    })
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}