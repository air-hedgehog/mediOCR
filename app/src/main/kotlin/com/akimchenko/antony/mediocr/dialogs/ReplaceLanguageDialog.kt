package com.akimchenko.antony.mediocr.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.NotificationCenter

class ReplaceLanguageDialog : DialogFragment() {

    companion object {
        const val ARG_NEW_LANGUAGE = "arg_new_language"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity as MainActivity? ?: return super.onCreateDialog(savedInstanceState)
        val recyclerView = RecyclerView(activity)
        recyclerView.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = ChosenLangsAdapter()
        return AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.choose_language_to_replace))
                .setView(recyclerView)
                .create()
    }

    private inner class ChosenLangsAdapter : RecyclerView.Adapter<ChosenLangsAdapter.ViewHolder>() {

        private inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            init {
                itemView.setOnClickListener {
                    val newLang = arguments?.getString(ARG_NEW_LANGUAGE, null) ?: return@setOnClickListener
                    val position = adapterPosition
                    if (position < 0 || position >= itemCount) return@setOnClickListener
                    AppSettings.replaceSelectedLanguage(newLang, position)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
                ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false))

        override fun getItemCount(): Int = AppSettings.getSelectedLanguageList().size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        NotificationCenter.notify(NotificationCenter.LANGUAGE_REPLACED)
    }
}