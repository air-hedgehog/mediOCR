package com.akimchenko.antony.mediocr

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

class MainActivity : AppCompatActivity() {

    private val permissionCallbacks: SparseArray<OnRequestPermissionCallback> = SparseArray()

    interface OnRequestPermissionCallback {
        fun onPermissionReturned(isGranted: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pushFragment(MainFragment())
    }

    fun pushFragment(fragment: Fragment) {
        if (isFinishing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed) return
        //hideKeyboard()
        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.main_activity_container, fragment, fragment.javaClass.name)
                .addToBackStack(fragment.javaClass.name)
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

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
    }
}
