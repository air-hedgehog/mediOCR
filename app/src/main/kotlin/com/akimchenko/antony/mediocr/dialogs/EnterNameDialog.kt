package com.akimchenko.antony.mediocr.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import java.util.*


class EnterNameDialog : DialogFragment() {

    companion object {
        const val FILE_TYPE_ID_ARG = "file_type_id_arg"
        private const val SAVED_STATE_TITLE = "saved_state_title"
    }

    private lateinit var editText: EditText
    private var fileTypeId: Int = NotificationCenter.SAVE_AS_TXT_ID

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity as MainActivity? ?: return super.onCreateDialog(savedInstanceState)
        fileTypeId =
            arguments?.getInt(FILE_TYPE_ID_ARG, NotificationCenter.SAVE_AS_TXT_ID) ?: (savedInstanceState?.getInt(
                FILE_TYPE_ID_ARG) ?: NotificationCenter.SAVE_AS_TXT_ID)
        editText = LayoutInflater.from(activity).inflate(R.layout.dialog_enter_name, null, false) as EditText

        val savedInstanceTitle = savedInstanceState?.getString(SAVED_STATE_TITLE)
        if (savedInstanceTitle != null) {
            editText.post {
                editText.setText(savedInstanceTitle)
                editText.setSelection(savedInstanceTitle.length)
            }
        }
        val currentDateName = Utils.formatDate(Calendar.getInstance().timeInMillis)
        editText.hint = currentDateName
        return AlertDialog.Builder(activity)
            .setPositiveButton(activity.getString(R.string.save)) { dialog, _ ->

                var editedText = editText.text.trim().toString()
                if (editedText.isEmpty())
                    editedText = currentDateName
                NotificationCenter.notify(fileTypeId, editedText)

                dialog.dismiss()
            }.setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setView(editText)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVED_STATE_TITLE, editText.text.toString())
        outState.putInt(FILE_TYPE_ID_ARG, fileTypeId)
    }

    override fun onResume() {
        super.onResume()
        editText.post {
            editText.requestFocus()
            val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun getFileTypeId() = fileTypeId

    fun getTitle(): String = editText.text.toString().trim()

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager? ?: return
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}