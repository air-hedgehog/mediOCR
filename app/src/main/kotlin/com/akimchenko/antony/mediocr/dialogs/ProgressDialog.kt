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

class ProgressDialog : DialogFragment() {

    companion object {
        const val INITIAL_TEXT_ARG = "initial_text_arg"
    }

    private var textView: TextView? = null

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_progress, null, false)
        textView = view.findViewById(R.id.text_view)
        setMessage(arguments?.getString(INITIAL_TEXT_ARG))
        val dialog = AlertDialog.Builder(activity)
            .setView(view)
            .create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    fun setMessage(message: String?) {
        textView?.text = message
    }

    fun getMessage(): String? = textView?.text.toString()
}