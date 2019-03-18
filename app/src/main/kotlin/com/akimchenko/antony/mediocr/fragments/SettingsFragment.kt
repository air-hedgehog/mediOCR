package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import kotlinx.android.synthetic.main.fragment_recycler.*

class SettingsFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_recycler, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        toolbar_title.text = activity.getString(R.string.settings)
        back_button.setOnClickListener { activity.onBackPressed() }
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.adapter = SettingsAdapter()

    }

    private inner class SettingsAdapter: RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

        abstract inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            abstract fun updateUI(position: Int)
        }

        private inner class LanguageViewHolder(itemView: View): ViewHolder(itemView) {

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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsAdapter.ViewHolder {
            return LanguageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_setting, parent, false))
        }

        override fun getItemCount() = 1

        override fun onBindViewHolder(holder: SettingsAdapter.ViewHolder, position: Int) = holder.updateUI(position)

    }
}