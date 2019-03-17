package com.akimchenko.antony.mediocr.adapters

import android.app.DownloadManager
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.fragments.LanguageFragment
import java.io.File


class LanguageDownloadAdapter(fragment: LanguageFragment, var items: ArrayList<String>) :
        RecyclerView.Adapter<LanguageDownloadAdapter.ViewHolder>(), LanguageFragment.OnDownloadStatusListener {

    val activity: MainActivity? = fragment.activity as MainActivity?
    val downloadIdsLangs: HashMap<Long, String> = HashMap()

    init {
        fragment.registerDownloadListener(this)
    }

    override fun statousChanged(language: String) {
        notifyItemChanged(items.indexOf(language))
    }

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun updateUI(position: Int)
    }

    private inner class AvailableLangViewHolder(itemView: View) : ViewHolder(itemView) {

        val downloadButton: ImageView = itemView.findViewById(R.id.download_button)


        init {
            downloadButton.setOnClickListener {
                val activity = activity ?: return@setOnClickListener
                val item = items[adapterPosition] as String? ?: return@setOnClickListener
                val file = File(activity.getTesseractDataFolder(), "$item.traineddata")
                if (!file.exists())
                    file.createNewFile()
                //TODO convert langCode to Language name
                download(items[adapterPosition], file, item)
            }
        }

        override fun updateUI(position: Int) {
            val activity = activity ?: return
            val item: String? = items[position] as String? ?: return
            val file = activity.getTesseractDataFolder().listFiles()?.find { it.name == "$item.traineddata" }
            val isDownloaded = file != null
            downloadButton.isClickable = !isDownloaded
            downloadButton.isFocusable = !isDownloaded
        }
    }

    private fun download(lang: String, destFile: File, fileName: String) {
        val activity = activity ?: return
        val request = DownloadManager.Request(Uri.parse("https://github.com/tesseract-ocr/tessdata/blob/master/$lang.traineddata?raw=true"))
                .setTitle(fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(FileProvider.getUriForFile(activity, activity.applicationContext.packageName + ".provider", destFile))
                .setAllowedOverRoaming(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            request.setAllowedOverMetered(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            request.setRequiresCharging(false)

        val downloadManager = activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager? ?: return
        downloadIdsLangs[downloadManager.enqueue(request)] = lang
    }

    private inner class DownloadedLangViewHolder(itemView: View) : ViewHolder(itemView) {

        init {

        }

        override fun updateUI(position: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageDownloadAdapter.ViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: LanguageDownloadAdapter.ViewHolder, position: Int) = holder.updateUI(position)
}