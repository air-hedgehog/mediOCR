package com.akimchenko.antony.mediocr.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.Utils
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.android.synthetic.main.fragment_preview.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class PreviewFragment : Fragment() {

    companion object {
        //TODO download specific language and initialize TessBaseAPI with lang from its name
        const val lang = "eng"
        const val TESSDATA = "tessdata"
        const val ARG_IMAGE_FILE_URI = "arg_image_file"
    }

    private var isRotated: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        val uriString = arguments?.getString(ARG_IMAGE_FILE_URI) ?: return
        val uri: Uri = Uri.parse(uriString)
        val imageFile: File
        when {
            uri.scheme == "file" -> imageFile = File(uri.path)
            uri.scheme == "content" -> {
                val inputStream = activity.contentResolver.openInputStream(uri) ?: return
                imageFile = activity.getFileForBitmap()
                val outputStream = FileOutputStream(imageFile)
                val buffer = ByteArray(1024)

                while ((inputStream.read(buffer)) > 0)
                    outputStream.write(buffer)

                inputStream.close()
                outputStream.close()
            }
            else -> return
        }


        image_view.setImageBitmap(BitmapFactory.decodeFile(imageFile.absolutePath))

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
        rotate_left_button.setOnClickListener {
            isRotated = true
            image_view.setImageBitmap(image_view.drawable.toBitmap().rotate(-90.0f))
        }
        rotate_right_button.setOnClickListener {
            isRotated = true
            image_view.setImageBitmap(image_view.drawable.toBitmap().rotate(90.0f))
        }
        recognise_button.setOnClickListener {
            if (isRotated) {
                isRotated = false
                //saving current rotation of bitmap into file, if it was rotated
                activity.showProgress()
                GlobalScope.launch {
                    if (imageFile.exists())
                        imageFile.delete()
                    imageFile.createNewFile()
                    Utils.writeBitmapToFile(image_view.drawable.toBitmap(), imageFile)
                }.invokeOnCompletion {
                    recognise(imageFile.toUri())
                }
            }
        }
    }

    private fun recognise(fileUri: Uri) {
        val activity = activity as MainActivity? ?: return
        activity.showProgress()
        var result: String? = null
        GlobalScope.launch {
            prepareTesseract()
            result = startOCR(fileUri)
        }.invokeOnCompletion {
            activity.hideProgress()
            if (result != null) {
                val resultFragment = ResultFragment()
                val args = Bundle()
                args.putString(ResultFragment.ARG_OCR_RESULT, result)
                resultFragment.arguments = args
                activity.pushFragment(resultFragment)
            }
        }
    }

    private fun getTesseractDataFolder(context: Context) = File(Utils.getInternalDirs(context)[0], TESSDATA)

    private fun prepareTesseract() {
        try {
            val activity = activity as MainActivity? ?: return
            val assets = activity.assets ?: return
            val fileList = assets.list(TESSDATA) ?: return

            val tessDataDir = getTesseractDataFolder(activity)
            if (!tessDataDir.exists() || !tessDataDir.isDirectory)
                tessDataDir.mkdir()

            for (fileName in fileList) {

                val existingAsset = File(tessDataDir, fileName)
                if (!existingAsset.exists()) {
                    val inputStream = assets.open("$TESSDATA/$fileName")
                    val outputStream = FileOutputStream(existingAsset)
                    val buffer = ByteArray(1024)

                    while ((inputStream.read(buffer)) > 0)
                        outputStream.write(buffer)

                    inputStream.close()
                    outputStream.close()

                }
            }
        } catch (e: IOException) {
            Log.e(this::class.java.name, e.message)
        }
    }

    private fun startOCR(fileUri: Uri): String? {
        try {
            val options = BitmapFactory.Options()
            options.inSampleSize = 4
            val bitmap = BitmapFactory.decodeFile(fileUri.path, options)
            return extractText(bitmap)
        } catch (e: Exception) {
            Log.e(this::class.java.name, e.message)
        }
        return null
    }

    private fun extractText(bitmap: Bitmap): String? {
        val activity = activity as MainActivity? ?: return null
        val tessBaseApi = TessBaseAPI()
        val path: String? = Utils.getInternalDirs(activity)[0]?.path ?: return null

        tessBaseApi.init(path, lang)
        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "×⦂⁃‐‑‒�–⎯—―~⁓•°%‰‱&⅋§÷±‼¡¿⸮⁇⁉⁈‽⸘¼½¾²³⅕⅙⅛©®™℠℻℅℁⅍¶⁋≠√�∛∜∞βΦΣ♀♂⚢⚣⌘♲♻☺★↑↓")

        Log.d(this::class.java.name, "Training file loaded")
        tessBaseApi.setImage(bitmap)
        var extractedText = "empty result"
        try {
            extractedText = tessBaseApi.utF8Text
        } catch (e: Exception) {
            Log.e(this::class.java.name, "Error in recognizing text.")
        }

        tessBaseApi.end()
        return extractedText
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap =
            Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)

}