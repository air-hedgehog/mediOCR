package com.akimchenko.antony.mediocr.adapters

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.fragments.MainFragment
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File


class MainFragmentAdapter(private val fragment: MainFragment) : RecyclerView.Adapter<MainFragmentAdapter.ViewHolder>(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val items: ArrayList<File> = ArrayList()
    private var searchQuery: String? = null
    private var fileLoadJob: Job? = null
    private val activity = fragment.activity as MainActivity?

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val title: TextView = itemView.findViewById(R.id.text_view)
        private val shareButton: ImageButton = itemView.findViewById(R.id.share_button)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)
        val icon: ImageView = itemView.findViewById(R.id.image_view)

        init {
            title.isSelected = true

            itemView.setOnClickListener {
                Intent(Intent.ACTION_VIEW).apply {
                    activity ?: return@apply
                    val position = adapterPosition
                    if (position < 0 || position >= items.size) return@setOnClickListener
                    val file = items[position] as File? ?: return@setOnClickListener
                    val fileUri = FileProvider.getUriForFile(
                        activity,
                        activity.applicationContext.packageName + ".provider",
                        file
                    )
                    val mimeType = activity.contentResolver.getType(fileUri)
                    this.setDataAndType(fileUri, mimeType)
                    this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    activity.startActivity(this)
                }
            }
            deleteButton.setOnClickListener {
                activity ?: return@setOnClickListener
                val position = adapterPosition
                if (position < 0 || position >= items.size) return@setOnClickListener
                val file = items[position]
                AlertDialog.Builder(activity)
                    .setMessage("${activity.getString(R.string.do_you_want_to_delete)} ${file.name}")
                    .setPositiveButton(activity.getString(R.string.delete)) { dialog, _ ->
                        deleteItem(items[position])
                        dialog.dismiss()
                    }.setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }.create().show()

            }

            activity?.let { activity -> shareButton.setOnClickListener { Utils.shareFile(activity, items[adapterPosition]) } }
        }
    }

    fun resume() {
        updateItems()
        AppSettings.registerListener(this)
    }

    fun pause() {
        AppSettings.unregisterListener(this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < 0 || position >= items.size) return
        val name = items[position].name
        holder.title.text = name
        activity?.let { activity ->
            holder.icon.setImageDrawable(
                    ContextCompat.getDrawable(
                            activity,
                            if (name.endsWith(".txt")) R.drawable.ic_txt else R.drawable.ic_pdf
                    )
            )
        }
    }

    private fun updateItems() {
        activity ?: return
        fragment.updateProgressBar(true)
        fileLoadJob = GlobalScope.launch {
            val files = activity.getDefaultSavedFilesDirectory().listFiles()
            items.clear()
            if (files != null) {
                if (searchQuery.isNullOrBlank())
                    items.addAll(files)
                else
                    items.addAll(files.filter { it.name.contains(searchQuery!!, true) })
                if (AppSettings.savedFilesSortedAlphabetically)
                    items.sortBy { it.name }
                else
                    items.sortByDescending { it.lastModified() }
            }
        }
        fileLoadJob?.invokeOnCompletion {
            activity.runOnUiThread {
                notifyDataSetChanged()
                fragment.updateProgressBar(false)
            }
        }
    }

    fun deleteItem(file: File) {
        if (!items.contains(file)) return
        file.delete()
        notifyItemRemoved(items.indexOf(file))
        items.remove(file)
        fragment.updateProgressBar(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_main_file, parent, false))

    override fun getItemCount() = items.size

    fun setSearchQuery(query: String?) {
        searchQuery = query
        updateItems()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == AppSettings.SAVED_FILES_SORT_TYPE)
            updateItems()
    }

}