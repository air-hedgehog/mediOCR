package com.akimchenko.antony.mediocr.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.akimchenko.anton.mediocr.R
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.Utils
import com.akimchenko.antony.mediocr.viewModels.LanguageViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class ItemViewType {
    ITEM_TYPE_LANGUAGE,
    ITEM_TYPE_SEPARATOR
}

val diffUtil = object :
    DiffUtil.ItemCallback<LanguageItem>() {
    override fun areItemsTheSame(oldItem: LanguageItem, newItem: LanguageItem): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: LanguageItem, newItem: LanguageItem): Boolean {
        return oldItem.type == newItem.type && oldItem.title == newItem.title
    }
}

class LanguageDownloadAdapter :
    ListAdapter<LanguageItem, LanguageDownloadAdapter.ViewHolder>(diffUtil), KoinComponent {

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun updateUI(position: Int)
    }

    private val viewModel: LanguageViewModel by inject()

    private inner class AvailableLangViewHolder(itemView: View) : ViewHolder(itemView) {

        private val deleteButton: ImageView = itemView.findViewById(R.id.delete_button)
        private val title: TextView = itemView.findViewById(R.id.text_view)
        private val checkMark: ImageView = itemView.findViewById(R.id.checkmark)
        private val progressbar: ProgressBar = itemView.findViewById(R.id.progress_bar)

        init {
            itemView.setOnClickListener {
                val item = getItem(adapterPosition) ?: return@setOnClickListener
                viewModel.download(item)
            }

            deleteButton.setOnClickListener {
                val item = getItem(adapterPosition) ?: return@setOnClickListener
                viewModel.delete(item)
                notifyItemChanged(adapterPosition)
            }
        }

        override fun updateUI(position: Int) {
            val item = getItem(position) ?: return

            if (!viewModel.repository.preinstalledLanguage(item)) {
                progressbar.isVisible = viewModel.repository.ongoingDownloadLanguages.values.contains(item)
                deleteButton.isVisible = viewModel.repository.isDownloaded(item) && progressbar.isVisible.not()

            } else {
                deleteButton.isVisible = false
                progressbar.isVisible = false
            }
            title.text = Utils.getLocalizedLangName(item)
            checkMark.isVisible = AppSettings.getSelectedLanguage() == item.title
        }
    }

    private inner class SeparatorViewHolder(itemView: View) : ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.text_view)

        override fun updateUI(position: Int) {
            if (position < 0 || position >= itemCount) return
            title.text = getItem(position).title
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ItemViewType.ITEM_TYPE_SEPARATOR.ordinal ->
                SeparatorViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_separator, parent, false)
                )

            ItemViewType.ITEM_TYPE_LANGUAGE.ordinal ->
                AvailableLangViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_language, parent, false)
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
        return getItem(position).type.ordinal

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.updateUI(position)

}

data class LanguageItem(val type: ItemViewType, val title: String)