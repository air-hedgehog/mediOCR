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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.fragments.LanguageFragment
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException


class LanguageDownloadAdapter(private val fragment: LanguageFragment) :
        RecyclerView.Adapter<LanguageDownloadAdapter.ViewHolder>(), NotificationCenter.Observer {

    companion object {
        private const val ITEM_TYPE_LANGUAGE = 0
        private const val ITEM_TYPE_SEPARATOR = 1
        private const val ITEM_TYPE_CHOSEN_LANGUAGE = 2
    }

    private var items = ArrayList<Item>()
    private var searchQuery: String? = null
    private val activity = fragment.activity as MainActivity?
    private var downloadJob: Job? = null
    private val itemsList = ArrayList<Item>()

    init {
        updateItems(true)
    }

    private fun updateItems(isForce: Boolean = false) {
        activity ?: return
        downloadJob = GlobalScope.launch {
            activity.runOnUiThread {
                fragment.updateProgressBar(true)
            }
            if (isForce) {
                itemsList.clear()
                try {
                    Jsoup.connect(activity.getString(R.string.tessdata_url)).timeout(6 * 1000).get().run {
                        this.select("td.content").forEach { element ->
                            val reference = element.getElementsByClass("js-navigation-open").attr("title")

                            if (reference.endsWith(activity.getString(R.string.traineddata_suffix)))
                                itemsList.add(Item(ITEM_TYPE_LANGUAGE, reference.removeSuffix(activity.getString(R.string.traineddata_suffix))))
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    connectionErrorAction()
                } catch (e: UnknownHostException) {
                    connectionErrorAction()
                }
            }

            items = if (searchQuery.isNullOrBlank())
                separateList(activity, itemsList)
            else
                separateList(activity, itemsList.filter { it.title != null && it.title.contains(searchQuery!!, true) })
        }
        downloadJob?.invokeOnCompletion {
            activity.runOnUiThread {
                notifyDataSetChanged()
                fragment.updateProgressBar(false)
            }
            downloadJob = null
        }
    }

    private fun connectionErrorAction() {
        val activity = activity ?: return
        val downloadedFiles = activity.getTesseractDataFolder().listFiles()?.filter { it.name.endsWith(activity.getString(R.string.traineddata_suffix)) }
        downloadedFiles?.forEach { itemsList.add(Item(ITEM_TYPE_LANGUAGE, it.name.removeSuffix(activity.getString(R.string.traineddata_suffix)))) }

        activity.runOnUiThread {
            Toast.makeText(activity, activity.getString(R.string.internet_error_occurred), Toast.LENGTH_LONG).show()
        }
    }

    private fun separateList(activity: MainActivity, list: List<Item>): ArrayList<Item> {
        return ArrayList<Item>().also { rv ->

            val chosenLangs = AppSettings.getSelectedLanguageList()
            val downloadedLangs = ArrayList<Item>()
            val availableLangs = ArrayList<Item>()

            list.forEach {
                if (!chosenLangs.contains(it.title)) {
                    if (Utils.isLanguageDownloaded(activity, it.title!!))
                        downloadedLangs.add(it)
                    else
                        availableLangs.add(it)
                }
            }

            rv.add(Item(ITEM_TYPE_SEPARATOR, activity.getString(R.string.chosen_languages)))

            for (i in 0..2) {
                val item = try {
                    Item(ITEM_TYPE_CHOSEN_LANGUAGE, chosenLangs[i])
                } catch (e: IndexOutOfBoundsException) {
                    Item(ITEM_TYPE_CHOSEN_LANGUAGE, null)
                }
                rv.add(item)
            }

            if (downloadedLangs.isNotEmpty()) {
                rv.add(Item(ITEM_TYPE_SEPARATOR, activity.getString(R.string.downloaded)))
                rv.addAll(downloadedLangs)
            }

            if (availableLangs.isNotEmpty()) {
                rv.add(Item(ITEM_TYPE_SEPARATOR, activity.getString(R.string.available)))
                rv.addAll(availableLangs)
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
        when (id) {
            NotificationCenter.LANG_DELETED -> {
                if (`object` == AppSettings.getSelectedLanguageList().contains(`object` as String)) {
                    AppSettings.removeSelectedLanguage(`object`)
                }
                updateItems()
            }
            NotificationCenter.LANG_DOWNLOADED -> updateItems()
        }
    }

    private inner class AvailableLangViewHolder(itemView: View) : ViewHolder(itemView) {

        val downloadDeleteButton: ImageView = itemView.findViewById(R.id.download_button)
        val title: TextView = itemView.findViewById(R.id.text_view)
        val progressbar: ProgressBar = itemView.findViewById(R.id.progress_bar)

        init {
            itemView.setOnClickListener {
                activity ?: return@setOnClickListener
                val lang = getLang(adapterPosition) ?: return@setOnClickListener

                if (!Utils.isLanguageDownloaded(activity, lang) && lang != "eng")
                    download(lang, File(activity.getTesseractDataFolder(), "$lang.traineddata"))

                if (AppSettings.getSelectedLanguageList().size < 3) {
                    AppSettings.addSelectedLanguage(lang)
                } else {
                    //TODO select language to replace alertDialog
                }
                notifyDataSetChanged()
            }
            downloadDeleteButton.setOnClickListener {
                activity ?: return@setOnClickListener
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
                notifyItemChanged(adapterPosition)
            }
        }

        private fun getLang(position: Int): String? {
            if (position < 0 || position >= items.size) return null
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
            title.text = Utils.getLocalizedLangName(lang)
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
                SeparatorViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_separator, parent, false))

            ITEM_TYPE_CHOSEN_LANGUAGE,
            ITEM_TYPE_LANGUAGE ->
                AvailableLangViewHolder(
                        LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
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
        return items[position.coerceIn(0, items.size - 1)].type
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.updateUI(position)

    fun setQuery(query: String?) {
        searchQuery = query
        updateItems()
    }

    private inner class Item(val type: Int, val title: String?)
}