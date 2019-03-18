package com.akimchenko.antony.mediocr.adapters

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


class MainFragmentAdapter(private val activity: MainActivity, private var items: ArrayList<File> = ArrayList()) :
        RecyclerView.Adapter<MainFragmentAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val title: TextView = itemView.findViewById(R.id.text_view)
        private val shareButton: ImageButton = itemView.findViewById(R.id.share_button)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)
        val icon: ImageView = itemView.findViewById(R.id.image_view)

        init {
            title.isSelected = true

            itemView.setOnClickListener {
                Intent(Intent.ACTION_VIEW).also { intent ->
                    val file = items[adapterPosition] as File? ?: return@setOnClickListener
                    val fileUri = FileProvider.getUriForFile(activity, activity.applicationContext.packageName + ".provider", file)
                    val mimeType = activity.contentResolver.getType(fileUri)

                    intent.setDataAndType(fileUri, mimeType)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    //intent.putExtra(Intent.EXTRA_STREAM, fileUri)
                    activity.startActivity(intent)
                }
            }
            deleteButton.setOnClickListener { deleteItem(items[adapterPosition]) }
            shareButton.setOnClickListener {
                Intent(Intent.ACTION_SEND).also { intent ->
                    val file = items[adapterPosition]
                    intent.type = "application/${if (file.name.endsWith(".pdf")) "pdf" else "txt"}"
                    val fileUri = FileProvider.getUriForFile(activity, activity.applicationContext.packageName + ".provider", file)
                    intent.putExtra(Intent.EXTRA_STREAM, fileUri)
                    activity.startActivity(Intent.createChooser(intent, null))
                }
            }
        }
    }

    override fun onBindViewHolder(holder: MainFragmentAdapter.ViewHolder, position: Int) {
        val name = items[position].name
        holder.title.text = name
        holder.icon.setImageDrawable(ContextCompat.getDrawable(activity, if (name.endsWith(".txt")) R.drawable.ic_txt else R.drawable.ic_pdf))
    }

    public fun updateItems(items: ArrayList<File>?) {
        if (items != null) {
            this.items = items
            this.items.sortByDescending { it.lastModified() }
        } else
            this.items.clear()
        notifyDataSetChanged()
    }

    public fun deleteItem(file: File) {
        if (!items.contains(file)) return
        items.remove(file)
        notifyItemRemoved(items.indexOf(file))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainFragmentAdapter.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_main_file, parent, false))

    override fun getItemCount() = items.size


}