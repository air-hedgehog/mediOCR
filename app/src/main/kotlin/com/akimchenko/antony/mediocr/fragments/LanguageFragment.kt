package com.akimchenko.antony.mediocr.fragments

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.adapters.LanguageDownloadAdapter
import kotlinx.android.synthetic.main.fragment_download_language.*


class LanguageFragment : Fragment() {

    public interface OnDownloadStatusListener {
        fun statousChanged(language: String)
    }

    private lateinit var adapter: LanguageDownloadAdapter
    private var downloadListener: OnDownloadStatusListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_download_language, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        recycler_view.layoutManager = LinearLayoutManager(activity)
        val languages = activity.resources.getStringArray(R.array.tessdata_langs)
        adapter = LanguageDownloadAdapter(this, ArrayList<String>().also { it.addAll(languages) })
        recycler_view.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    fun registerDownloadListener(listener: OnDownloadStatusListener) {
        this.downloadListener = listener
    }

    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(onDownloadComplete)
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (adapter.downloadIdsLangs.containsKey(id)) {
                val language = adapter.downloadIdsLangs[id]!!
                downloadListener?.statousChanged(language)
                adapter.downloadIdsLangs.remove(id)
                Toast.makeText(activity, "Download Completed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}