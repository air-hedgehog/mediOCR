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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.akimchenko.anton.mediocr.R
import com.akimchenko.antony.mediocr.fragments.MainFragment
import com.akimchenko.antony.mediocr.fragments.PreviewFragment
import org.koin.android.ext.android.inject
import java.io.File
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    val repository: AppRepository by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (supportFragmentManager.backStackEntryCount == 0)
            pushFragment(MainFragment())

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.type != "image/*") return


        val permissionsGranted: Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
            }

        fun parsePicture() {
            var scheme: String? = intent.scheme
            var uriString: String? = intent.dataString
            if (scheme == null) {
                val receiveUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as Uri?
                }
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
        }

        if (permissionsGranted) {
            parsePicture()
        } else {
            //TODO request permissions
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(onDownloadComplete)
    }

    fun pushFragment(fragment: Fragment) {
        if (isFinishing || isDestroyed) return
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
            repository.notifyLanguageDownloaded(id)
        }
    }

    fun getFileForBitmap() =
        File("${repository.getDefaultCroppedImagesDirectory()}/${Calendar.getInstance().timeInMillis}.jpg")
}
