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
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import java.io.File
import java.util.*


class LanguageDownloadAdapter(val activity: MainActivity) :
        RecyclerView.Adapter<LanguageDownloadAdapter.ViewHolder>(), NotificationCenter.Observer {

    private var items = ArrayList<String>()
    private var searchQuery: String? = null

    init {
        buildList()
    }

    private fun buildList() {
        val list = arrayListOf("eng")
        list.addAll(activity.resources.getStringArray(R.array.tessdata_langs))

        items = if (searchQuery.isNullOrBlank())
            list
        else
            list.filter { Utils.getLocalizedLangName(it).contains(searchQuery!!, true) } as ArrayList<String>

        notifyDataSetChanged()
    }

    fun resume() {
        NotificationCenter.addObserver(this)
        notifyDataSetChanged()
    }

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

    private inner class AvailableLangViewHolder(itemView: View) : ViewHolder(itemView) {

        val downloadDeleteButton: ImageView = itemView.findViewById(R.id.download_button)
        val title: TextView = itemView.findViewById(R.id.text_view)
        val checkMark: ImageView = itemView.findViewById(R.id.checkmark)
        val progressbar: ProgressBar = itemView.findViewById(R.id.progress_bar)

        init {
            itemView.setOnClickListener {
                val item = items[adapterPosition] as String? ?: return@setOnClickListener
                if (!Utils.isLanguageDownloaded(activity, item) && item != "eng")
                    download(item, File(activity.getTesseractDataFolder(), "$item.traineddata"), Utils.getLocalizedLangName(item))

                AppSettings.setSelectedLanguage(item)
                notifyDataSetChanged()
            }
            downloadDeleteButton.setOnClickListener {
                val item = items[adapterPosition] as String? ?: return@setOnClickListener
                val file = File(activity.getTesseractDataFolder(), "$item.traineddata")
                if (Utils.isLanguageDownloaded(activity, item)) {
                    AlertDialog.Builder(activity)
                            .setMessage("${activity.getString(R.string.do_you_want_to_delete)} ${Utils.getLocalizedLangName(item)}")
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
            val item = items[position] as String? ?: return
            if (item != "eng") {
                val isDownloaded = Utils.isLanguageDownloaded(activity, item)
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
            } else {
                downloadDeleteButton.visibility = View.GONE
                progressbar.visibility = View.GONE
            }
            title.text = Utils.getLocalizedLangName(item)
            val isSelected = AppSettings.getSelectedLanguage() == item
            checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
            title.setPadding(if (isSelected) 0 else activity.resources.getDimensionPixelSize(R.dimen.default_side_margin), 0, 0, 0)
        }
    }

    private fun download(lang: String, destFile: File, fileName: String) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return AvailableLangViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.updateUI(position)

    fun setQuery(query: String?) {
        searchQuery = query
        buildList()
    }
}