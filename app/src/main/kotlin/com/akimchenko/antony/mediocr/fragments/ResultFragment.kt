package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import kotlinx.android.synthetic.main.fragment_result.*

class ResultFragment : Fragment() {

    companion object {
        const val ARG_OCR_RESULT = "arg_ocr_result"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        val resultString: String? = arguments?.getString(ARG_OCR_RESULT) ?: return
        back_button.setOnClickListener { activity.onBackPressed() }
        edit_text.setText(resultString)
    }
}