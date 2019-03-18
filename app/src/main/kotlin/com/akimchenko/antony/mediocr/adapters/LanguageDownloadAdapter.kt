package com.akimchenko.antony.mediocr.adapters

import android.app.DownloadManager
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
        val title: TextView = itemView.findViewById(R.id.text_view)
        val checkmark: ImageView = itemView.findViewById(R.id.checkmark)

        init {
            downloadButton.setOnClickListener {
                activity ?: return@setOnClickListener
                val item = items[adapterPosition] as String? ?: return@setOnClickListener
                val isDownloaded = activity.getTesseractDataFolder().listFiles()?.find { it.name == "$item.traineddata" } != null
                val file = File(activity.getTesseractDataFolder(), "$item.traineddata")
                if (isDownloaded) {
                    //TODO delete confirmation
                    if (file.exists())
                        file.delete()
                    notifyItemChanged(adapterPosition)
                } else {
                    //TODO convert langCode to Language name
                    download(items[adapterPosition], file, item)
                }
            }
        }

        override fun updateUI(position: Int) {
            activity ?: return
            val item: String? = items[position] as String? ?: return

            //TODO convert langCode to Language name
            title.text = item
            val isDownloaded = activity.getTesseractDataFolder().listFiles()?.find { it.name == "$item.traineddata" } != null
            //TODO non-clickable progressbar
            downloadButton.setImageDrawable(ContextCompat.getDrawable(activity, if (isDownloaded) R.drawable.delete else R.drawable.download))
        }
    }

    private fun download(lang: String, destFile: File, fileName: String) {
        activity ?: return
        val request = DownloadManager.Request(Uri.parse("https://github.com/tesseract-ocr/tessdata/blob/master/$lang.traineddata?raw=true"))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(destFile))
                //FileProvider.getUriForFile(activity, activity.applicationContext.packageName + ".provider", destFile)
                .setAllowedOverRoaming(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            request.setAllowedOverMetered(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            request.setRequiresCharging(false)

        val downloadManager = activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager? ?: return
        downloadIdsLangs[downloadManager.enqueue(request)] = lang
    }

  /*  private inner class DownloadedLangViewHolder(itemView: View) : ViewHolder(itemView) {

        init {

        }

        override fun updateUI(position: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }*/

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageDownloadAdapter.ViewHolder {
        return AvailableLangViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: LanguageDownloadAdapter.ViewHolder, position: Int) = holder.updateUI(position)
}