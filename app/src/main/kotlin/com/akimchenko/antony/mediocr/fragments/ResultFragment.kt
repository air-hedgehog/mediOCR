package com.akimchenko.antony.mediocr.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.Utils
import kotlinx.android.synthetic.main.fragment_result.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.*

class ResultFragment : Fragment() {

    companion object {
        const val ARG_OCR_RESULT = "arg_ocr_result"
        const val SAVE_AS_TXT_ID = 0
        const val SAVE_AS_PDF_ID = 1
        const val SAVE_AS_DOCX_ID = 2
    }

    private var counter: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        val resultString = arguments?.getString(ARG_OCR_RESULT) ?: return
        close_button.setOnClickListener { activity.popFragment(MainFragment::class.java.name) }
        save_button.setOnClickListener {
            val popup: PopupMenu = PopupMenu(activity, save_button).also { popup ->
                popup.menu.add(0, SAVE_AS_TXT_ID, 0, activity.getString(R.string.save_as_txt))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    popup.menu.add(0, SAVE_AS_PDF_ID, 1, activity.getString(R.string.save_as_pdf))
                popup.menu.add(0, SAVE_AS_DOCX_ID, 2, activity.getString(R.string.save_as_docx))
            }
            popup.setOnMenuItemClickListener {
                showEnterNameAlert(activity, edit_text.text.trim().toString(), it.itemId)
                false
            }
            popup.show()
        }
        share_button.setOnClickListener {
            counter = 0
            startActivity(Intent(Intent.ACTION_SEND).also { intent ->
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, edit_text.text.toString())
            })
        }
        edit_text.setText(resultString)
    }

    @SuppressLint("InflateParams")
    private fun showEnterNameAlert(activity: MainActivity, contentText: String, fileTypeId: Int) {
        val editTextView: EditText =
            LayoutInflater.from(activity).inflate(R.layout.dialog_enter_name, null, false) as EditText
        val currentDateName = Utils.formatDate(Calendar.getInstance().timeInMillis)
        editTextView.hint = currentDateName
        AlertDialog.Builder(activity)
            .setView(editTextView)
            .setPositiveButton(activity.getString(R.string.save)) { dialog, _ ->

                var editedText = editTextView.text.trim().toString()
                if (editedText.isEmpty())
                    editedText = currentDateName
                when (fileTypeId) {
                    SAVE_AS_TXT_ID -> saveAsTxt(activity, contentText, editedText)
                    SAVE_AS_PDF_ID -> saveAsPdf(activity, contentText, editedText)
                    SAVE_AS_DOCX_ID -> saveAsDocx(activity, contentText, editedText)
                }

                dialog.dismiss()
            }.setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }.create().show()
    }

    private fun saveAsDocx(activity: MainActivity, text: String, name: String) {

    }

    private fun saveAsPdf(activity: MainActivity, text: String, name: String) {
        //val pdfDoc = PdfDocument()
    }

    private fun saveAsTxt(activity: MainActivity, text: String, name: String) {
        val defaultDir = activity.getDefaultSavedFilesDirectory()
        val suffix = ".txt"
        val increment = getIncrementForNameRecursive(defaultDir, name, suffix)
        val uniqueName = "$name${if (increment > 0) "($increment)" else ""}$suffix"
        val file = File(defaultDir, uniqueName)

        if (!file.exists())
            file.createNewFile()
        val output = FileOutputStream(file)
        val osw = OutputStreamWriter(output)
        osw.append(text)
        osw.close()
        output.flush()
        output.close()
    }

    private fun getIncrementForNameRecursive(directory: File, name: String, suffix: String): Int {
        val existingFile: File? = directory.listFiles().find {
            val countedName: String = if (counter > 0) "$name($counter)" else name
            it.name.removeSuffix(suffix) == countedName
        }
        if (existingFile != null) {
            counter++
            getIncrementForNameRecursive(directory, name, suffix)
        }

        return counter
    }
}