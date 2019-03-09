package com.akimchenko.antony.mediocr

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment(), View.OnClickListener {

    companion object {
        const val READ_WRITE_CAMERA_REQUEST_CODE = 101
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity: MainActivity? = activity as MainActivity?
        activity ?: return
        //recycler_view.layoutManager = GridLayoutManager(activity, 2)
        camera_button.setImageDrawable(Utils.makeSelector(activity, ContextCompat.getDrawable(activity, R.drawable.camera_button)!!.toBitmap()))
        gallery_button.setImageDrawable(Utils.makeSelector(activity, ContextCompat.getDrawable(activity, R.drawable.gallery_button)!!.toBitmap()))
        camera_button.setOnClickListener(this)
        gallery_button.setOnClickListener(this)
    }

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
                                activity.pushFragment(CameraFragment())
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
                //TODO GalleryView
            }
        }
    }

}