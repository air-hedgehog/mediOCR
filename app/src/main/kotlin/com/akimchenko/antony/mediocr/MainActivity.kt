package com.akimchenko.antony.mediocr

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.SparseArray
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.akimchenko.antony.mediocr.fragments.MainFragment
import kotlinx.android.synthetic.main.dialog_progress.view.*
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private val permissionCallbacks: SparseArray<OnRequestPermissionCallback> = SparseArray()
    private var progressDialog: AlertDialog? = null
    private var progressMessage: String? = null

    interface OnRequestPermissionCallback {
        fun onPermissionReturned(isGranted: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pushFragment(MainFragment())
    }

    fun pushFragment(fragment: Fragment) {
        if (isFinishing || Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed) return
        //hideKeyboard()
        val name = fragment::class.java.name
        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.main_activity_container, fragment, name)
                .addToBackStack(name)
                .commitAllowingStateLoss()
    }

    fun requestPermissions(strings: Array<String>, requestCode: Int, callback: OnRequestPermissionCallback) {
        var allGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            strings.forEach {
                if (ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    return@forEach
                }
            }
        }

        if (allGranted) {
            callback.onPermissionReturned(true)
        } else {
            permissionCallbacks.put(requestCode, callback)
            ActivityCompat.requestPermissions(this, strings, requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var allGranted = true
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                return@forEach
            }
        }

        val callback: OnRequestPermissionCallback? = permissionCallbacks.get(requestCode, null)
        callback?.onPermissionReturned(allGranted)
        permissionCallbacks.remove(requestCode)
    }

    override fun onBackPressed() {
        if (isFinishing) return
        val entryCount = supportFragmentManager.backStackEntryCount
        val fragment =
                supportFragmentManager.findFragmentByTag(supportFragmentManager.getBackStackEntryAt(entryCount - 1).name)
        if (fragment is MainFragment || entryCount == 0)
            finish()
        else
            super.onBackPressed()
    }

    fun popFragment(fragmentName: String) {
        supportFragmentManager.popBackStackImmediate(fragmentName, 0)
    }

    @SuppressLint("InflateParams")
    fun showProgress(message: String? = null) {
        runOnUiThread {
            if (progressDialog != null && message != progressMessage)
                hideProgress()
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null, false)
            progressMessage = message
            view.text_view.text = message ?: getString(R.string.please_wait)
            progressDialog = AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create()
            progressDialog?.setCanceledOnTouchOutside(false)
            progressDialog?.show()
        }
    }

    fun hideProgress() {
        runOnUiThread {
            progressDialog?.dismiss()
            progressDialog = null
            progressMessage = null
        }
    }

    fun getFileForBitmap() = File("${getDefaultCroppedImagesDirectory()}/${Calendar.getInstance().timeInMillis}.jpg")

    fun getDefaultSavedFilesDirectory(): File {
        val savedFilesDirectory = File(getDefaultDirectory(), getString(R.string.saved_files))
        if (!savedFilesDirectory.exists() || !savedFilesDirectory.isDirectory)
            savedFilesDirectory.mkdirs()
        return savedFilesDirectory
    }

    private fun getDefaultCroppedImagesDirectory(): File {
        val croppedImageDirectory = File(getDefaultDirectory(), getString(R.string.cropped_images))
        if (!croppedImageDirectory.exists() || !croppedImageDirectory.isDirectory)
            croppedImageDirectory.mkdirs()
        return croppedImageDirectory
    }

    private fun getDefaultDirectory(): File {
        val defaultDirectory = File(Environment.getExternalStorageDirectory(), getString(R.string.default_folder_name))
        if (!defaultDirectory.exists() || !defaultDirectory.isDirectory)
            defaultDirectory.mkdirs()
        return defaultDirectory
    }
}
