package com.akimchenko.antony.mediocr.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R

@SuppressLint("ValidFragment")
class ProgressDialog(var initialText: String?) : DialogFragment() {

    private var textView: TextView? = null

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity as MainActivity? ?: return super.onCreateDialog(savedInstanceState)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_progress, null, false)
        textView = view.findViewById(R.id.text_view)
        if (initialText != null) {
            setMessage(initialText)
            initialText = null
        }
        return AlertDialog.Builder(activity)
            .setView(view)
            .create()
    }

    fun setMessage(message: String?) {
        textView?.text = message
    }

    fun getMessage(): String? = textView?.text.toString()
}