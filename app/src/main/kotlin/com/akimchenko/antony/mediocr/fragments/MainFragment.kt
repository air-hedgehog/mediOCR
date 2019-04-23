package com.akimchenko.antony.mediocr.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.adapters.MainFragmentAdapter
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.Utils
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File


@SuppressLint("InlinedApi")
class MainFragment : BaseFragment(), View.OnClickListener {

    private var newPhotoFile: File? = null

    companion object {
        const val READ_WRITE_CAMERA_REQUEST_CODE = 101
        const val CAPTURE_IMAGE_REQUEST_CODE = 102
        const val GALLERY_CHOOSER_REQUEST_CODE = 103
        const val READ_WRITE_REQUEST_CODE = 104
    }

    private var adapter: MainFragmentAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        activity.setSupportActionBar(toolbar)
        setHasOptionsMenu(true)
        recycler_view.layoutManager = GridLayoutManager(
            activity, if (activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
            ) 2 else 3
        )
        adapter = MainFragmentAdapter(activity)
        recycler_view.adapter = adapter
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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu ?: return
        inflater ?: return
        return inflater.inflate(R.menu.main_screen_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val activity = activity as MainActivity? ?: return false
        when (item?.itemId) {
            R.id.settings -> activity.pushFragment(SettingsFragment())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        adapter?.updateItems()
        hint.visibility = if (adapter != null && adapter!!.itemCount > 0) View.GONE else View.VISIBLE
    }

    override fun onClick(v: View?) {
        val activity = activity as MainActivity? ?: return
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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && AppSettings.useApplicationCamera) {
                                    val manager =
                                        activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager? ?: return
                                    var isCamera2Supported = false

                                    for (cameraId in manager.cameraIdList) {
                                        val characteristics: Int? = manager.getCameraCharacteristics(cameraId)
                                            .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                                        isCamera2Supported =
                                            characteristics == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                                        break
                                    }

                                    if (isCamera2Supported)
                                        activity.pushFragment(CameraFragment())
                                    else
                                        sendCameraIntent()
                                } else
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
            gallery_button -> {
                activity.requestPermissions(arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), READ_WRITE_REQUEST_CODE, object : MainActivity.OnRequestPermissionCallback {
                    override fun onPermissionReturned(isGranted: Boolean) {
                        if (isGranted)
                            sendGalleryChooserIntent()
                        else
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.you_need_to_allow_permissions_read_write),
                                Toast.LENGTH_LONG
                            ).show()
                    }
                })
            }
        }
    }

    private fun sendCameraIntent() {
        val activity = activity as MainActivity? ?: return
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(activity.packageManager)?.apply {
                newPhotoFile = activity.getFileForBitmap()
                if (!newPhotoFile!!.exists())
                    newPhotoFile!!.createNewFile()
                newPhotoFile!!.apply {
                    val uri = FileProvider.getUriForFile(
                        activity,
                        activity.applicationContext.packageName + ".provider",
                        newPhotoFile!!
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST_CODE)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        recycler_view.layoutManager =
            GridLayoutManager(activity, if (newConfig?.orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 3)
    }

    private fun sendGalleryChooserIntent() {
        Intent().apply {
            this.type = "image/*"
            this.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(this, null), GALLERY_CHOOSER_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val activity = activity as MainActivity? ?: return
        when (requestCode) {
            CAPTURE_IMAGE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && newPhotoFile != null) {
                    activity.pushFragment(PreviewFragment().also { fragment ->
                        fragment.arguments = Bundle().also { args ->
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
                    activity.pushFragment(PreviewFragment().also { fragment ->
                        fragment.arguments = Bundle().also { args ->
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