package com.akimchenko.antony.mediocr

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.SparseArray
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.akimchenko.antony.mediocr.dialogs.ProgressDialog
import com.akimchenko.antony.mediocr.fragments.BaseFragment
import com.akimchenko.antony.mediocr.fragments.MainFragment
import com.akimchenko.antony.mediocr.fragments.PreviewFragment
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.MyDownloadManager
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private val permissionCallbacks: SparseArray<OnRequestPermissionCallback> = SparseArray()
    private var progressDialog: ProgressDialog? = null
    val downloadIdsLangs: HashMap<Long, String> = HashMap()

    interface OnRequestPermissionCallback {
        fun onPermissionReturned(isGranted: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalScope.launch {
            MyDownloadManager.getInitialDownloadList(this@MainActivity).let { list ->
                if (list.isNotEmpty() && list != AppSettings.initialDownloadsList) {
                    //getting list of downloadable tessdata o cube data
                    AppSettings.initialDownloadsList = list.filter {
                        it.contains(getString(R.string.traineddata_suffix)) || it.contains("cube")
                    }
                }
            }
        }

        setContentView(R.layout.activity_main)

        if (supportFragmentManager.backStackEntryCount == 0)
            pushFragment(MainFragment())

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        if (intent.type == "image/*") {
            requestPermissions(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), Utils.READ_WRITE_INTENT_REQUEST_CODE, object : OnRequestPermissionCallback {
                override fun onPermissionReturned(isGranted: Boolean) {
                    if (isGranted) {
                        var scheme: String? = intent.scheme
                        var uriString: String? = intent.dataString
                        if (scheme == null) {
                            val receiveUri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as Uri?
                            uriString = receiveUri.toString()
                            scheme = receiveUri?.scheme
                        }
                        scheme ?: return
                        uriString ?: return
                        when (scheme) {
                            "content",
                            "file" -> {
                                pushFragment(PreviewFragment().also { fragment ->
                                    fragment.arguments = Bundle().also { args ->
                                        args.putString(
                                            PreviewFragment.ARG_IMAGE_FILE_URI,
                                            uriString
                                        )
                                    }
                                })
                                setIntent(null)
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.you_need_to_allow_permissions_read_write),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })

        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        downloadIdsLangs.keys.forEach { key ->
            downloadIdsLangs[key]?.let {
                if (Utils.isLanguageDownloaded(this, it))
                    downloadIdsLangs.remove(key)
            }
        }
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(onDownloadComplete)
    }

    fun pushFragment(fragment: Fragment) {
        if (isFinishing || Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed) return
        val name = fragment::class.java.name
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.main_activity_container, fragment, name)
            .addToBackStack(name)
            .commitAllowingStateLoss()
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadIdsLangs.containsKey(id)) {
                val language = downloadIdsLangs[id] ?: return
                NotificationCenter.notify(NotificationCenter.LANG_DOWNLOADED, language)
                downloadIdsLangs.remove(id)
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.download_completed)}: ${Utils.getLocalizedLangName(
                        language
                    )}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun requestPermissions(
        strings: Array<String>,
        requestCode: Int,
        callback: OnRequestPermissionCallback
    ) {
        var allGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            strings.forEach {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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

        if (supportFragmentManager.backStackEntryCount <= 0)
            finish()
        else {
            val fragment = supportFragmentManager.fragments.last() as BaseFragment?
            fragment?.onBackPressed()
        }
    }

    fun popFragment(fragmentName: String) {
        supportFragmentManager.popBackStackImmediate(fragmentName, 0)
    }

    fun showProgress(message: String? = getString(R.string.please_wait)) {
        runOnUiThread {
            if (progressDialog?.dialog == null || progressDialog?.dialog?.isShowing == false) {
                progressDialog = ProgressDialog().also { dialog ->
                    dialog.arguments = Bundle().apply {
                        this.putString(ProgressDialog.INITIAL_TEXT_ARG, message)
                        dialog.show(
                            this@MainActivity.supportFragmentManager,
                            ProgressDialog::class.java.name
                        )
                    }
                }
            }
            if (message != progressDialog?.getMessage())
                progressDialog?.setMessage(message)
        }
    }

    fun hideProgress() {
        runOnUiThread {
            progressDialog?.dismiss()
            progressDialog = null
        }
    }

    fun getFileForBitmap() =
        File("${getDefaultCroppedImagesDirectory()}/${Calendar.getInstance().timeInMillis}.jpg")

    fun getTesseractDataFolder(): File {
        val dir = File(Utils.getInternalDirs(this)[0], PreviewFragment.TESSDATA)
        if (!dir.exists())
            dir.mkdirs()
        return dir
    }

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
        val defaultDirectory =
            File(Environment.getExternalStorageDirectory(), getString(R.string.default_folder_name))
        if (!defaultDirectory.exists() || !defaultDirectory.isDirectory)
            defaultDirectory.mkdirs()
        return defaultDirectory
    }
}
