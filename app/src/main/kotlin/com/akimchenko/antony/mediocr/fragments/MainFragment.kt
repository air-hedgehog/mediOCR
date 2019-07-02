package com.akimchenko.antony.mediocr.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
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
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar_progress_bar.*
import java.io.File


@SuppressLint("InlinedApi")
class MainFragment(override val layoutResId: Int = R.layout.fragment_main) : BaseSearchFragment(), View.OnClickListener {

    private var newPhotoFile: File? = null

    private var adapter: MainFragmentAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        toolbar.title = activity.getString(R.string.app_name)
        recycler_view.layoutManager = GridLayoutManager(
            activity, if (activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
            ) 2 else 3
        )
        adapter = MainFragmentAdapter(this)
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

    override fun onBackPressed() {
        activity?.finish()
    }

    fun updateProgressBar(isVisible: Boolean) {
        progress_bar.visibility = if (isVisible) View.VISIBLE else View.GONE
        hint.visibility = if (adapter != null && adapter!!.itemCount > 0) View.GONE else View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu?.add(0, ITEM_SETTINGS, menu.size(), R.string.settings)?.setIcon(R.drawable.settings)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu?.add(0, ITEM_SORT_TYPE_TITLE, menu.size(), R.string.sort_by_name)?.setIcon(R.drawable.sort_alphabetically)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu?.add(0, ITEM_SORT_TYPE_DATE, menu.size(), R.string.sort_by_date)?.setIcon(R.drawable.sort_by_date)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        adapter?.setSearchQuery(newText)
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val activity = activity as MainActivity? ?: return false
        when (item?.itemId) {
            ITEM_SETTINGS -> activity.pushFragment(SettingsFragment())
            ITEM_SORT_TYPE_TITLE -> AppSettings.savedFilesSortedAlphabetically = true
            ITEM_SORT_TYPE_DATE -> AppSettings.savedFilesSortedAlphabetically = false
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        adapter?.resume()
    }

    override fun onPause() {
        super.onPause()
        adapter?.pause()
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
                    Utils.READ_WRITE_CAMERA_REQUEST_CODE,
                    object : MainActivity.OnRequestPermissionCallback {
                        override fun onPermissionReturned(isGranted: Boolean) {
                            if (isGranted)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && AppSettings.useApplicationCamera) {
                                    if (Utils.isCamera2APISupported(activity))
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
                ), Utils.READ_WRITE_GALLERY_REQUEST_CODE, object : MainActivity.OnRequestPermissionCallback {
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
                    startActivityForResult(takePictureIntent, Utils.CAPTURE_IMAGE_REQUEST_CODE)
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
            startActivityForResult(Intent.createChooser(this, null), Utils.GALLERY_CHOOSER_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val activity = activity as MainActivity? ?: return
        when (requestCode) {
            Utils.CAPTURE_IMAGE_REQUEST_CODE -> {
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
            Utils.GALLERY_CHOOSER_REQUEST_CODE -> {
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