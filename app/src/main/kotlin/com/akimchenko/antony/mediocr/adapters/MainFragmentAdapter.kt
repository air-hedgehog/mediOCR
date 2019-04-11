package com.akimchenko.antony.mediocr.adapters

import android.app.AlertDialog
import android.content.Intent
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
import java.io.File


class MainFragmentAdapter(private val activity: MainActivity) : RecyclerView.Adapter<MainFragmentAdapter.ViewHolder>() {

    private val items: ArrayList<File> = ArrayList()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val title: TextView = itemView.findViewById(R.id.text_view)
        private val shareButton: ImageButton = itemView.findViewById(R.id.share_button)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)
        val icon: ImageView = itemView.findViewById(R.id.image_view)

        init {
            title.isSelected = true

            itemView.setOnClickListener {
                Intent(Intent.ACTION_VIEW).apply {
                    val file = items[adapterPosition] as File? ?: return@setOnClickListener
                    val fileUri = FileProvider.getUriForFile(activity, activity.applicationContext.packageName + ".provider", file)
                    val mimeType = activity.contentResolver.getType(fileUri)
                    this.setDataAndType(fileUri, mimeType)
                    this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    activity.startActivity(this)
                }
            }
            deleteButton.setOnClickListener {
                val file = items[adapterPosition]
                AlertDialog.Builder(activity)
                        .setMessage("${activity.getString(R.string.do_you_want_to_delete)} ${file.name}")
                        .setPositiveButton(activity.getString(R.string.delete)) { dialog, _ ->
                            deleteItem(items[adapterPosition])
                            dialog.dismiss()
                        }.setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }.create().show()

            }

            shareButton.setOnClickListener {
                Intent(Intent.ACTION_SEND).apply {
                    val file = items[adapterPosition]
                    val fileUri = FileProvider.getUriForFile(activity, activity.applicationContext.packageName + ".provider", file)
                    this.type = activity.contentResolver.getType(fileUri)
                    this.putExtra(Intent.EXTRA_STREAM, fileUri)
                    activity.startActivity(Intent.createChooser(this, null))
                }
            }
        }
    }

    override fun onBindViewHolder(holder: MainFragmentAdapter.ViewHolder, position: Int) {
        val name = items[position].name
        holder.title.text = name
        holder.icon.setImageDrawable(ContextCompat.getDrawable(activity, if (name.endsWith(".txt")) R.drawable.ic_txt else R.drawable.ic_pdf))
    }

    fun updateItems() {
        val files = activity.getDefaultSavedFilesDirectory().listFiles() as Array<File>? ?: return
        items.clear()
        items.addAll(files)
        notifyDataSetChanged()
    }

    fun deleteItem(file: File) {
        if (!items.contains(file)) return
        file.delete()
        notifyItemRemoved(items.indexOf(file))
        items.remove(file)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainFragmentAdapter.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_main_file, parent, false))

    override fun getItemCount() = items.size

}