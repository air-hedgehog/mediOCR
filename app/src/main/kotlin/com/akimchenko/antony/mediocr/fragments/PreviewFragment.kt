package com.akimchenko.antony.mediocr.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.Utils
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.android.synthetic.main.fragment_preview.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class PreviewFragment : Fragment() {

    companion object {
        //TODO download specific language and initialize TessBaseAPI with lang from its name
        const val lang = "eng"
        const val TESSDATA = "tessdata"
        const val ARG_IMAGE_FILE = "arg_image_file"
    }

    private lateinit var defaultDirectory: File

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
        defaultDirectory = File("${Environment.getExternalStorageDirectory()}/${activity.getString(R.string.default_folder_name)}")
        if (!defaultDirectory.exists() || !defaultDirectory.isDirectory)
            defaultDirectory.mkdirs()

        close_button.setImageDrawable(
            Utils.makeSelector(
                activity,
                ContextCompat.getDrawable(activity, R.drawable.close)!!.toBitmap()
            )
        )
        rotate_left_button.setImageDrawable(
            Utils.makeSelector(
                activity,
                ContextCompat.getDrawable(activity, R.drawable.rotate_left)!!.toBitmap()
            )
        )
        rotate_right_button.setImageDrawable(
            Utils.makeSelector(
                activity,
                ContextCompat.getDrawable(activity, R.drawable.rotate_right)!!.toBitmap()
            )
        )
        recognise_button.setImageDrawable(
            Utils.makeSelector(
                activity,
                ContextCompat.getDrawable(activity, R.drawable.recognition_button)!!.toBitmap()
            )
        )

        close_button.setOnClickListener { activity.popFragment(MainFragment::class.java.name) }
        rotate_left_button.setOnClickListener { image_view.setImageBitmap(image_view.drawable.toBitmap().rotate(90.0f)) }
        rotate_right_button.setOnClickListener { image_view.setImageBitmap(image_view.drawable.toBitmap().rotate(-90.0f)) }
        recognise_button.setOnClickListener {
            //TODO tesseract recognition
        }
    }

    private fun recognise(filePath: String) {
        prepareTesseract()
        startOCR(filePath)
    }

    private fun prepareTesseract() {
        try {
            val assets = activity?.assets ?: return
            val fileList = assets.list(TESSDATA) ?: return
            for (fileName in fileList) {

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                val existingAsset = File(defaultDirectory.path, TESSDATA)
                if (!existingAsset.exists()) {

                    val inputStream = assets.open("$TESSDATA/$fileName")

                    val outputStream = FileOutputStream(existingAsset)

                    // Transfer bytes from in to out
                    val buf = ByteArray(1024)
                    var len = inputStream.read(buf)

                    while (len > 0) {
                        len = inputStream.read(buf)
                        outputStream.write(buf, 0, len)
                    }
                    inputStream.close()
                    outputStream.close()

                }
            }
        } catch (e: IOException) {
            //Log.e(TAG, e.stackTrace)
        }
    }

    private fun copyTessDataFiles(path: String) {


    }

    //TODO asyncTask (Corutines)
    private fun startOCR(filePath: String) {
        try {
            val options = BitmapFactory.Options()
            options.inSampleSize =
                4 // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.
            val bitmap = BitmapFactory.decodeFile(filePath, options)

            //TODO pass result to result fragment to edit and copy
            val result = extractText(bitmap)

        } catch (e: Exception) {
            //Log.e(TAG, e.message)
        }

    }

    private fun extractText(bitmap: Bitmap): String? {
        val tessBaseApi: TessBaseAPI = TessBaseAPI()

        tessBaseApi.init(defaultDirectory.path, lang)

        //       //EXTRA SETTINGS
        //        //For example if we only want to detect numbers
        //        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890");
        //
        //        //blackList Example
        //        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
        //                "YTRWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");

        //Log.d(TAG, "Training file loaded")
        tessBaseApi.setImage(bitmap)
        var extractedText = "empty result"
        try {
            extractedText = tessBaseApi.utF8Text
        } catch (e: Exception) {
            //Log.e(TAG, "Error in recognizing text.")
        }

        tessBaseApi.end()
        return extractedText
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap =
        Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)

}