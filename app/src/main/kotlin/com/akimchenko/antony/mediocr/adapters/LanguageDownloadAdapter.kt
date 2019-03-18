package com.akimchenko.antony.mediocr.adapters

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.fragments.LanguageFragment
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import java.io.File
import java.util.*


class LanguageDownloadAdapter(fragment: LanguageFragment, var items: ArrayList<String>) :
        RecyclerView.Adapter<LanguageDownloadAdapter.ViewHolder>(), NotificationCenter.Observer {


    val activity: MainActivity? = fragment.activity as MainActivity?

    fun resume() = NotificationCenter.addObserver(this)

    fun pause() = NotificationCenter.removeObserver(this)

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun updateUI(position: Int)
    }

    override fun onNotification(id: Int, `object`: Any?) {
        when (id) {
            NotificationCenter.LANG_DELETED -> {
                if (`object` == AppSettings.getSelectedLanguage())
                    AppSettings.setSelectedLanguage(null)
                notifyDataSetChanged()
            }
            NotificationCenter.LANG_DOWNLOAD_STATUS_CHANGED -> notifyItemChanged(items.indexOf(`object` as String))
        }
    }

    private fun getLocalizedLangName(item: String) = Utils.customLanguageTags[item] ?: Locale(item).displayLanguage

    private inner class AvailableLangViewHolder(itemView: View) : ViewHolder(itemView) {

        val downloadDeleteButton: ImageView = itemView.findViewById(R.id.download_button)
        val title: TextView = itemView.findViewById(R.id.text_view)
        val checkMark: ImageView = itemView.findViewById(R.id.checkmark)
        val progressbar: ProgressBar = itemView.findViewById(R.id.progress_bar)

        private fun isLanguageDownloaded(item: String): Boolean {
            activity ?: return false
            return activity.getTesseractDataFolder().listFiles()?.find { it.name == "$item.traineddata" } != null
        }

        init {
            itemView.setOnClickListener {
                activity ?: return@setOnClickListener
                val item = items[adapterPosition] as String? ?: return@setOnClickListener
                val isDownloaded = isLanguageDownloaded(item)
                if (!isDownloaded)
                    download(item, File(activity.getTesseractDataFolder(), "$item.traineddata"), getLocalizedLangName(item))

                AppSettings.setSelectedLanguage(item)
                notifyDataSetChanged()
            }
            downloadDeleteButton.setOnClickListener {
                activity ?: return@setOnClickListener
                val item = items[adapterPosition] as String? ?: return@setOnClickListener
                val isDownloaded = isLanguageDownloaded(item)
                val file = File(activity.getTesseractDataFolder(), "$item.traineddata")
                if (isDownloaded) {
                    AlertDialog.Builder(activity)
                            .setMessage("${activity.getString(R.string.do_you_want_to_delete)} ${getLocalizedLangName(item)}")
                            .setPositiveButton(activity.getString(R.string.delete)) { dialog, _ ->
                                if (file.exists())
                                    file.delete()
                                NotificationCenter.notify(NotificationCenter.LANG_DELETED, item)
                                dialog.dismiss()
                            }.setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                                dialog.dismiss()
                            }.create().show()

                } else {
                    download(items[adapterPosition], file, item)
                }
                notifyItemChanged(adapterPosition)
            }
        }

        override fun updateUI(position: Int) {
            activity ?: return
            val item = items[position] as String? ?: return

            title.text = getLocalizedLangName(item)
            val isDownloaded = isLanguageDownloaded(item)
            val isDownloading = activity.downloadIdsLangs.containsValue(item)
            downloadDeleteButton.isClickable = !isDownloading
            downloadDeleteButton.isFocusable = !isDownloading
            if (isDownloading) {
                downloadDeleteButton.visibility = View.GONE
                progressbar.visibility = View.VISIBLE
            } else {
                downloadDeleteButton.visibility = View.VISIBLE
                progressbar.visibility = View.GONE
            }
            downloadDeleteButton.setImageDrawable(ContextCompat.getDrawable(activity, if (isDownloaded) R.drawable.delete else R.drawable.download))

            val isSelected = AppSettings.getSelectedLanguage() == item
            checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
            title.setPadding(if (isSelected) 0 else activity.resources.getDimensionPixelSize(R.dimen.default_side_margin), 0, 0, 0)
        }
    }

    private fun download(lang: String, destFile: File, fileName: String) {
        activity ?: return
        val request = DownloadManager.Request(Uri.parse("https://github.com/tesseract-ocr/tessdata/blob/master/$lang.traineddata?raw=true"))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setTitle(fileName)
                .setDestinationUri(Uri.fromFile(destFile))
                .setAllowedOverRoaming(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            request.setAllowedOverMetered(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            request.setRequiresCharging(false)

        val downloadManager = activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager?
                ?: return
        activity.downloadIdsLangs[downloadManager.enqueue(request)] = lang
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageDownloadAdapter.ViewHolder {
        return AvailableLangViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: LanguageDownloadAdapter.ViewHolder, position: Int) = holder.updateUI(position)
}