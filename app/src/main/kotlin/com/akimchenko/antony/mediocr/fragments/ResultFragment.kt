package com.akimchenko.antony.mediocr.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.Utils
import com.google.android.material.snackbar.Snackbar
import com.itextpdf.text.Document
import com.itextpdf.text.Font
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.android.synthetic.main.fragment_result.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.*


class ResultFragment : BaseFragment() {

    companion object {
        const val ARG_OCR_RESULT = "arg_ocr_result"
        const val SAVE_AS_TXT_ID = 0
        const val SAVE_AS_PDF_ID = 1
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
            counter = 0
            PopupMenu(activity, save_button).apply {
                this.menu.add(0, SAVE_AS_TXT_ID, 0, activity.getString(R.string.save_as_txt))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    this.menu.add(0, SAVE_AS_PDF_ID, 1, activity.getString(R.string.save_as_pdf))
                this.setOnMenuItemClickListener {
                    showEnterNameAlert(activity, edit_text.text.trim().toString(), it.itemId)
                    false
                }
                this.show()
            }
        }
        share_button.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SEND).apply {
                this.type = "text/plain"
                this.putExtra(Intent.EXTRA_TEXT, edit_text.text.toString())
            })
        }
        updateTextFormatting(resultString)
        formatting_switch.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.defaultResultFormatting = isChecked
            updateTextFormatting(resultString)
        }
        formatting_switch.isChecked = AppSettings.defaultResultFormatting
    }

    private fun updateTextFormatting(text: String) = edit_text.setText(if (AppSettings.defaultResultFormatting) text else text.removeRowBreaks())

    private fun String.removeRowBreaks(): String = this.replace("\n", " ", true)

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
                }

                dialog.dismiss()
            }.setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }.create().show()
    }

    @SuppressLint("NewApi")
    private fun saveAsPdf(activity: MainActivity, text: String, name: String) {
        val defaultDir = activity.getDefaultSavedFilesDirectory()
        val file = File(defaultDir, getUniqueName(defaultDir, name, ".pdf"))
        if (!file.exists())
            file.createNewFile()

        Document().apply {
            val fOut = FileOutputStream(file)
            PdfWriter.getInstance(this, fOut)
            this.open()
            val p1 = Paragraph(text)
            val paraFont = Font(Font.FontFamily.UNDEFINED)
            p1.alignment = Paragraph.ALIGN_LEFT
            p1.font = paraFont
            this.add(p1)
            this.close()
        }
        showSnackbar(activity, file)
    }

    private fun saveAsTxt(activity: MainActivity, text: String, name: String) {
        val defaultDir = activity.getDefaultSavedFilesDirectory()
        val file = File(defaultDir, getUniqueName(defaultDir, name, ".txt"))
        if (!file.exists())
            file.createNewFile()

        val output = FileOutputStream(file)
        OutputStreamWriter(output).apply {
            this.append(text)
            this.close()
        }
        output.flush()
        output.close()
        showSnackbar(activity, file)
    }

    private fun showSnackbar(context: Context, file: File) {
        Snackbar.make(pushable_layout, context.getString(R.string.saved),
            Snackbar.LENGTH_LONG).setAction(context.getString(R.string.share)) { Utils.shareFile(context, file) }.show()
    }

    private fun getUniqueName(defaultDir: File, name: String, suffix: String): String {
        val increment = getIncrementForNameRecursive(defaultDir, name, suffix)
        return "$name${if (increment > 0) "($increment)" else ""}$suffix"
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