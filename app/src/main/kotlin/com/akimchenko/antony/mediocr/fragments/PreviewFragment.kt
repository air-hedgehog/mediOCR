package com.akimchenko.antony.mediocr.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.Utils
import kotlinx.android.synthetic.main.fragment_preview.*
import java.io.File

class PreviewFragment : Fragment() {

    companion object {
        const val ARG_IMAGE_FILE = "arg_image_file"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        val imageFile: File
        val filePath: String? = arguments?.getString(ARG_IMAGE_FILE)
        if (filePath == null) {
            activity.popFragment(MainFragment::class.java.name)
            return
        } else {
            imageFile = File(filePath)
        }
        if (!imageFile.exists()) {
            activity.popFragment(MainFragment::class.java.name)
            return
        } else {
            image_view.setImageBitmap(BitmapFactory.decodeFile(imageFile.absolutePath))
        }
        close_button.setImageDrawable(Utils.makeSelector(activity, ContextCompat.getDrawable(activity, R.drawable.close)!!.toBitmap()))
        rotate_left_button.setImageDrawable(Utils.makeSelector(activity, ContextCompat.getDrawable(activity, R.drawable.rotate_left)!!.toBitmap()))
        rotate_right_button.setImageDrawable(Utils.makeSelector(activity, ContextCompat.getDrawable(activity, R.drawable.rotate_right)!!.toBitmap()))
        recognise_button.setImageDrawable(Utils.makeSelector(activity, ContextCompat.getDrawable(activity, R.drawable.recognition_button)!!.toBitmap()))

        close_button.setOnClickListener { activity.popFragment(MainFragment::class.java.name) }
        rotate_left_button.setOnClickListener { image_view.setImageBitmap(image_view.drawable.toBitmap().rotate(90.0f)) }
        rotate_right_button.setOnClickListener { image_view.setImageBitmap(image_view.drawable.toBitmap().rotate(-90.0f)) }
        recognise_button.setOnClickListener {
            //TODO tesseract recognition
            }
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap =
            Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)

}