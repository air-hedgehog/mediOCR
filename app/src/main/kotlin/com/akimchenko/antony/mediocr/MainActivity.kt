package com.akimchenko.antony.mediocr

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.akimchenko.antony.mediocr.fragments.MainFragment
import kotlinx.android.synthetic.main.dialog_progress.view.*

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

    var progressDialog: AlertDialog? = null
    @SuppressLint("InflateParams")
    fun showProgress(message: String? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null, false)
        view.text_view.text = message ?: getString(R.string.please_wait)
        progressDialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
        progressDialog?.setCanceledOnTouchOutside(false)
        progressDialog?.show()
    }

    fun hideProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

}
