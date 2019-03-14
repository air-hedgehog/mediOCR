package com.akimchenko.antony.mediocr.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import kotlinx.android.synthetic.main.fragment_result.*
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.*

class ResultFragment : Fragment() {

    companion object {
        const val ARG_OCR_RESULT = "arg_ocr_result"
        const val SAVE_AS_TXT_ID = 0
        const val SAVE_AS_PDF_ID = 1
        const val SAVE_AS_DOCX_ID = 2
    }

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
                popup.menu.add(0, SAVE_AS_PDF_ID, 1, activity.getString(R.string.save_as_pdf))
                popup.menu.add(0, SAVE_AS_DOCX_ID, 2, activity.getString(R.string.save_as_docx))
            }
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    SAVE_AS_TXT_ID -> saveAsTxt(activity, resultString)
                    SAVE_AS_PDF_ID -> {
                        //TODO pdf
                    }
                    SAVE_AS_DOCX_ID -> {
                    }
                }
                false
            }
            popup.show()
        }
        share_button.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SEND).also { intent ->
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, edit_text.text.toString())
            })
        }
        edit_text.setText(resultString)
    }

    private fun saveAsTxt(context: Context, text: String) {
        //TODO alertDialog to enter file name
        try {
            OutputStreamWriter(context.openFileOutput(Calendar.getInstance().timeInMillis.toString(), Context.MODE_PRIVATE)).also {
                it.write(text)
                it.close()
            }
        } catch (e: IOException) {
            Log.e(ResultFragment::class.java.name, e.message)
        }
    }
}