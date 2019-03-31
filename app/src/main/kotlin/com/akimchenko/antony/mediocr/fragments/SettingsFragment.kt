package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.AppSettings
import kotlinx.android.synthetic.main.fragment_recycler.*

class SettingsFragment : BaseFragment() {

    companion object {
        private const val ITEM_LANG_SETTINGS = 0
        private const val ITEM_DEFAULT_CAMERA_SWITCH = 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_recycler, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        toolbar_title.text = activity.getString(R.string.settings)
        back_button.setOnClickListener { activity.onBackPressed() }
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.adapter = SettingsAdapter()
        recycler_view.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))

    }

    private inner class SettingsAdapter : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

        private val items = arrayListOf(ITEM_LANG_SETTINGS, ITEM_DEFAULT_CAMERA_SWITCH)

        abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            abstract fun updateUI(position: Int)
        }

        private inner class LanguageViewHolder(itemView: View) : ViewHolder(itemView) {

            val icon: ImageView = itemView.findViewById(R.id.image_view)
            val title: TextView = itemView.findViewById(R.id.text_view)

            init {
                itemView.setOnClickListener {
                    val activity = activity as MainActivity? ?: return@setOnClickListener
                    activity.pushFragment(LanguageFragment())
                }
            }

            override fun updateUI(position: Int) {
                val activity = activity as MainActivity? ?: return
                icon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.language))
                title.text = activity.getString(R.string.download_languages)
            }
        }

        private inner class DefaultCameraViewHolder(itemView: View) : ViewHolder(itemView) {

            val icon: ImageView = itemView.findViewById(R.id.image_view)
            val switchView: Switch = itemView.findViewById(R.id.switch_view)

            init {
                itemView.setOnClickListener {
                    switchView.isChecked = !switchView.isChecked
                }
                switchView.setOnCheckedChangeListener { _, isChecked ->
                    AppSettings.useApplicationCamera = isChecked
                }
            }

            override fun updateUI(position: Int) {
                val activity = activity as MainActivity? ?: return
                icon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.shutter))
                switchView.isChecked = AppSettings.useApplicationCamera
                switchView.text = activity.getString(R.string.use_app_camera)
            }
        }

        override fun getItemViewType(position: Int): Int = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsAdapter.ViewHolder {
            return when (viewType) {
                ITEM_LANG_SETTINGS -> LanguageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_text_view_with_icon, parent, false)
                )
                ITEM_DEFAULT_CAMERA_SWITCH -> DefaultCameraViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_switch_with_con, parent, false)
                )
                else -> LanguageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_text_view_with_icon, parent, false)
                )
            }
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: SettingsAdapter.ViewHolder, position: Int) = holder.updateUI(position)

    }
}