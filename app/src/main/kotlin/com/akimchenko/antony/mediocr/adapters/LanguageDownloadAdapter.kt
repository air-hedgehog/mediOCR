package com.akimchenko.antony.mediocr.adapters

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
import com.akimchenko.antony.mediocr.utils.MyDownloadManager
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


class LanguageDownloadAdapter(fragment: LanguageFragment) :
    RecyclerView.Adapter<LanguageDownloadAdapter.ViewHolder>(), NotificationCenter.Observer {

    companion object {
        private const val ITEM_TYPE_LANGUAGE = 0
        private const val ITEM_TYPE_SEPARATOR = 1
    }

    private var items = ArrayList<Item>()
    private var searchQuery: String? = null
    private val activity = fragment.activity as MainActivity?
    private var downloadJob: Job? = null

    init {
        updateItems(true)
    }

    private fun updateItems(isForce: Boolean = false) {
        activity ?: return
        items.clear()
        if (isForce) {
            downloadJob = GlobalScope.launch {
                MyDownloadManager.getInitialDownloadList(activity).let { list ->
                    if (list.isNotEmpty() && list != AppSettings.initialDownloadsList) {
                        //getting list of downloadable tessdata o cube data
                        AppSettings.initialDownloadsList = list.filter {
                            it.contains(activity.getString(R.string.traineddata_suffix)) || it.contains("cube")
                        }
                    }
                }
            }
            downloadJob?.invokeOnCompletion {
                convertDownloadsToItems(activity)?.let { items.addAll(it) }
                activity.runOnUiThread { notifyDataSetChanged() }
            }
        } else {
            convertDownloadsToItems(activity)?.let { items.addAll(it) }
            notifyDataSetChanged()
        }
    }

    private fun convertDownloadsToItems(activity: MainActivity): List<Item>? {
        AppSettings.initialDownloadsList?.let { list ->

            return separateList(activity, ArrayList<Item>().apply {
                list.filter { it.endsWith(activity.getString(R.string.traineddata_suffix)) }
                    .forEach { this.add(Item(ITEM_TYPE_LANGUAGE, it)) }
            })
        }
        return null
    }

    private fun separateList(activity: MainActivity, list: List<Item>): ArrayList<Item> {
        return ArrayList<Item>().also { rv ->
            list.partition { item ->
                Utils.isLanguageDownloaded(activity, item.title)
            }.also { pair ->
                if (pair.first.isNotEmpty()) {
                    rv.add(Item(ITEM_TYPE_SEPARATOR, activity.getString(R.string.downloaded)))
                    rv.addAll(pair.first)
                }

                if (pair.second.isNotEmpty()) {
                    rv.add(Item(ITEM_TYPE_SEPARATOR, activity.getString(R.string.available)))
                    rv.addAll(pair.second)
                }
            }
        }
    }

    fun resume() {
        NotificationCenter.addObserver(this)
    }

    fun pause() {
        NotificationCenter.removeObserver(this)
        downloadJob?.cancel()
    }

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun updateUI(position: Int)
    }

    override fun onNotification(id: Int, `object`: Any?) {
        val activity = activity ?: return
        when (id) {
            NotificationCenter.LANG_DELETED -> {
                if (`object` == AppSettings.selectedLanguage)
                    AppSettings.selectedLanguage = "eng${activity.getString(R.string.traineddata_suffix)}"
                updateItems()
            }
            NotificationCenter.LANG_DOWNLOADED -> updateItems()
        }
    }

    private inner class AvailableLangViewHolder(itemView: View) : ViewHolder(itemView) {

        val downloadDeleteButton: ImageView = itemView.findViewById(R.id.download_button)
        val title: TextView = itemView.findViewById(R.id.text_view)
        val checkMark: ImageView = itemView.findViewById(R.id.checkmark)
        val progressbar: ProgressBar = itemView.findViewById(R.id.progress_bar)

        init {
            itemView.setOnClickListener {
                //TODO
                /*activity ?: return@setOnClickListener
                val lang = getLang(adapterPosition) ?: return@setOnClickListener

                if (!Utils.isLanguageDownloaded(activity, lang) && lang != "eng")
                    download(lang, File(activity.getTesseractDataFolder(), "$lang.traineddata"))

                AppSettings.setSelectedLanguage(lang)
                notifyDataSetChanged()*/
            }
            downloadDeleteButton.setOnClickListener {
                //TODO download with cube data if presented in initialDownloadList
               /* activity ?: return@setOnClickListener
                val lang = getLang(adapterPosition) ?: return@setOnClickListener
                val file = File(activity.getTesseractDataFolder(), "$lang.traineddata")
                if (Utils.isLanguageDownloaded(activity, lang)) {
                    AlertDialog.Builder(activity)
                        .setMessage(
                            "${activity.getString(R.string.do_you_want_to_delete)} ${Utils.getLocalizedLangName(
                                lang
                            )}"
                        )
                        .setPositiveButton(activity.getString(R.string.delete)) { dialog, _ ->
                            if (file.exists())
                                file.delete()
                            NotificationCenter.notify(NotificationCenter.LANG_DELETED, lang)
                            dialog.dismiss()
                        }.setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }.create().show()

                } else {
                    download(lang, file)
                }
                notifyItemChanged(adapterPosition)*/
            }
        }

        private fun getLang(position: Int): String? {
            if (position !in 0 until itemCount) return null
            return items[position].title
        }

        override fun updateUI(position: Int) {
            activity ?: return
            val lang = getLang(position) ?: return

            if (lang != "eng") {
                val isDownloaded = Utils.isLanguageDownloaded(activity, lang)
                val isDownloading = activity.downloadIdsLangs.containsValue(lang)
                downloadDeleteButton.isClickable = !isDownloading
                downloadDeleteButton.isFocusable = !isDownloading
                if (isDownloading) {
                    downloadDeleteButton.visibility = View.GONE
                    progressbar.visibility = View.VISIBLE
                } else {
                    downloadDeleteButton.visibility = View.VISIBLE
                    progressbar.visibility = View.GONE
                }
                downloadDeleteButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        activity,
                        if (isDownloaded) R.drawable.delete else R.drawable.download
                    )
                )
            } else {
                downloadDeleteButton.visibility = View.GONE
                progressbar.visibility = View.GONE
            }
            title.text = Utils.getLocalizedLangName(lang.removeSuffix(activity.getString(R.string.traineddata_suffix)))
            val isSelected = AppSettings.selectedLanguage == lang
            checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
            title.setPadding(if (isSelected) 0 else activity.resources.getDimensionPixelSize(R.dimen.default_side_margin), 0, 0, 0)
        }
    }

    private inner class SeparatorViewHolder(itemView: View) : ViewHolder(itemView) {

        val title: TextView = itemView.findViewById(R.id.text_view)

        override fun updateUI(position: Int) {
            if (position < 0 || position >= items.size) return
            title.text = items[position].title
        }
    }

    private fun download(lang: String, destFile: File) {
        activity ?: return
        val request =
            DownloadManager.Request(Uri.parse("${activity.getString(R.string.tessdata_url)}$lang.traineddata"))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setTitle(Utils.getLocalizedLangName(lang))
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
        return when (viewType) {
            ITEM_TYPE_SEPARATOR ->
                SeparatorViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_separator,
                        parent,
                        false
                    )
                )

            ITEM_TYPE_LANGUAGE ->
                AvailableLangViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_language,
                        parent,
                        false
                    )
                )

            else -> SeparatorViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_separator,
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.updateUI(position)

    fun setQuery(query: String?) {
        searchQuery = query
        updateItems()
    }

    private inner class Item(val type: Int, val title: String)
}