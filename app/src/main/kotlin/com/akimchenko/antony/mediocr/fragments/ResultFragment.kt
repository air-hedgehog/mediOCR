package com.akimchenko.antony.mediocr.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.dialogs.EnterNameDialog
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import com.google.android.material.snackbar.Snackbar
import com.itextpdf.text.Document
import com.itextpdf.text.Font
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.android.synthetic.main.fragment_result.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter


class ResultFragment : BaseFragment() {

    companion object {
        const val ARG_OCR_RESULT = "arg_ocr_result"
        private const val SHARE_BUTTON_ID = -1
    }

    private var counter: Int = 0
    private var enterNameDialog: EnterNameDialog? = null
    private var isFirstBackPressed: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        val resultString = arguments?.getString(ARG_OCR_RESULT) ?: return
        setHasOptionsMenu(true)
        toolbar.navigationIcon = ContextCompat.getDrawable(activity, R.drawable.close)
        toolbar.setNavigationOnClickListener {
            if (isFirstBackPressed) {
                activity.popFragment(MainFragment::class.java.name)
            } else {
                isFirstBackPressed = true
                Handler().postDelayed({ isFirstBackPressed = false }, 2000)
                Toast.makeText(activity, activity.getString(R.string.click_again_to_exit), Toast.LENGTH_SHORT).show()
            }
        }
        updateTextFormatting(resultString)
        formatting_switch.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.defaultResultFormatting = isChecked
            updateTextFormatting(resultString)
        }
        formatting_switch.isChecked = AppSettings.defaultResultFormatting
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu?.add(0, SHARE_BUTTON_ID, 0, R.string.share)?.setIcon(R.drawable.share)
                ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu?.add(0, NotificationCenter.SAVE_AS_TXT_ID, 1, R.string.save_as_txt)?.setIcon(R.drawable.save_as_txt)
                ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            menu?.add(0, NotificationCenter.SAVE_AS_PDF_ID, 1, R.string.save_as_pdf)?.setIcon(R.drawable.save_as_pdf)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            SHARE_BUTTON_ID -> {
                startActivity(Intent(Intent.ACTION_SEND).apply {
                    this.type = "text/plain"
                    this.putExtra(Intent.EXTRA_TEXT, edit_text.text.toString())
                })
                false
            }
            NotificationCenter.SAVE_AS_TXT_ID,
            NotificationCenter.SAVE_AS_PDF_ID -> {
                val activity = activity as MainActivity? ?: return false
                counter = 0
                showEnterNameAlert(activity, item.itemId)
                false
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateTextFormatting(text: String) =
            edit_text.setText(if (AppSettings.defaultResultFormatting) text else text.removeRowBreaks())

    private fun String.removeRowBreaks(): String = this.replace("\n", " ", true)

    @SuppressLint("InflateParams")
    private fun showEnterNameAlert(activity: MainActivity, fileTypeId: Int) {
        enterNameDialog = EnterNameDialog()
        enterNameDialog!!.arguments = Bundle().apply {
            this.putInt(EnterNameDialog.FILE_TYPE_ID_ARG, fileTypeId)
        }
        enterNameDialog!!.show(activity.supportFragmentManager, EnterNameDialog::class.java.name)
    }

    @SuppressLint("NewApi")
    private fun saveAsPdf(activity: MainActivity, name: String) {
        val defaultDir = activity.getDefaultSavedFilesDirectory()
        val file = File(defaultDir, getUniqueName(defaultDir, name, ".pdf"))
        if (!file.exists())
            file.createNewFile()

        Document().apply {
            val fOut = FileOutputStream(file)
            PdfWriter.getInstance(this, fOut)
            this.open()
            val p1 = Paragraph(edit_text.text.trim().toString())
            val paraFont = Font(Font.FontFamily.UNDEFINED)
            p1.alignment = Paragraph.ALIGN_LEFT
            p1.font = paraFont
            this.add(p1)
            this.close()
        }
        showSnackbar(activity, file)
    }

    override fun onNotification(id: Int, `object`: Any?) {
        super.onNotification(id, `object`)
        val activity = activity as MainActivity? ?: return
        when (id) {
            NotificationCenter.SAVE_AS_TXT_ID -> saveAsTxt(activity, `object` as String)
            NotificationCenter.SAVE_AS_PDF_ID -> saveAsPdf(activity, `object` as String)
        }
    }

    private fun saveAsTxt(activity: MainActivity, name: String) {
        val defaultDir = activity.getDefaultSavedFilesDirectory()
        val file = File(defaultDir, getUniqueName(defaultDir, name, ".txt"))
        if (!file.exists())
            file.createNewFile()

        val output = FileOutputStream(file)
        OutputStreamWriter(output).apply {
            this.append(edit_text.text.trim().toString())
            this.close()
        }
        output.flush()
        output.close()
        showSnackbar(activity, file)
    }

    private fun showSnackbar(context: Context, file: File) {
        Snackbar.make(
                pushable_layout, context.getString(R.string.saved),
                Snackbar.LENGTH_LONG
        ).setAction(context.getString(R.string.share)) { Utils.shareFile(context, file) }.show()
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