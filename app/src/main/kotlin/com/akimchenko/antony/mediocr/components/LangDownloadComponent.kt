package com.akimchenko.antony.mediocr.components

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import com.akimchenko.antony.mediocr.MediocrApp
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import java.util.HashMap

class LangDownloadComponent(val context: Context) {

    val downloadIdsLangs: HashMap<Long, String> = HashMap()

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadIdsLangs.containsKey(id)) {
                val language = downloadIdsLangs[id]!!
                NotificationCenter.notify(NotificationCenter.LANG_DOWNLOAD_STATUS_CHANGED, language)
                downloadIdsLangs.remove(id)
                Toast.makeText(
                    context,
                    "${context.getString(R.string.download_completed)}: ${Utils.getLocalizedLangName(language)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun register() = (context as MediocrApp).registerReceiver(onDownloadComplete,
        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
    )

    fun unregister() = (context as MediocrApp).unregisterReceiver(onDownloadComplete)


}