package com.akimchenko.antony.mediocr.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.AppSettingsComponent
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import com.edmodo.cropper.CropImageView
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.android.synthetic.main.fragment_preview.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class PreviewFragment : BaseFragment() {

    companion object {
        const val TESSDATA = "tessdata"
        const val ARG_IMAGE_FILE_URI = "arg_image_file"
    }

    private val appSettings = get<AppSettingsComponent>()

    private var doWhenDownloaded: Runnable? = null

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

        crop_image_view.setGuidelines(CropImageView.DEFAULT_GUIDELINES)
        crop_image_view.setImageBitmap(BitmapFactory.decodeFile(imageFile.absolutePath))

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
        language_button.text = Utils.getLocalizedLangName(appSettings.getSelectedLanguage())
        language_button.setOnClickListener { activity.pushFragment(LanguageFragment()) }

        close_button.setOnClickListener { activity.popFragment(MainFragment::class.java.name) }
        rotate_left_button.setOnClickListener {
            crop_image_view.rotateImage(-90)
        }
        rotate_right_button.setOnClickListener {
            crop_image_view.rotateImage(90)
        }

        recognise_button.setOnClickListener {
            val runnable = Runnable {
                activity.showProgress(activity.getString(R.string.saving_cropped_image))
                GlobalScope.launch {
                    if (imageFile.exists())
                        imageFile.delete()
                    imageFile.createNewFile()
                    Utils.writeBitmapToFile(crop_image_view.croppedImage, imageFile)
                }.invokeOnCompletion {
                    recognise(imageFile.toUri())
                }
            }
            updateProgressVisibility(true)
            if (activity.downloadIdsLangs.containsValue(appSettings.getSelectedLanguage()))
                doWhenDownloaded = runnable
            else
                runnable.run()
        }
    }

    override fun onResume() {
        super.onResume()
        val activity = activity as MainActivity? ?: return
        updateProgressVisibility(activity.downloadIdsLangs.containsValue(appSettings.getSelectedLanguage()))
    }

    private fun updateProgressVisibility(isVisible: Boolean) {
        val activity = activity as MainActivity? ?: return
        if (isVisible && appSettings.getSelectedLanguage() != "eng") {
            recognise_button.setColorFilter(ContextCompat.getColor(activity, R.color.selected_tint))
            progress_bar.visibility = View.VISIBLE
            recognise_button.isClickable = false
            recognise_button.isFocusable = false
        } else {
            recognise_button.clearColorFilter()
            progress_bar.visibility = View.GONE
            recognise_button.isClickable = true
            recognise_button.isFocusable = true
        }
    }

    private fun isLangDownloaded(activity: MainActivity): Boolean = activity.getTesseractDataFolder().listFiles()
            .contains(File(activity.getTesseractDataFolder(), "${appSettings.getSelectedLanguage()}.traineddata"))

    override fun onNotification(id: Int, `object`: Any?) {
        super.onNotification(id, `object`)
        if (id == NotificationCenter.LANG_DOWNLOAD_STATUS_CHANGED && (`object` as String) == appSettings.getSelectedLanguage()) {
            val activity = activity as MainActivity? ?: return
            updateProgressVisibility(!isLangDownloaded(activity))
            doWhenDownloaded?.run()
            doWhenDownloaded = null
        }
    }

    private fun recognise(fileUri: Uri) {
        val activity = activity as MainActivity? ?: return
        activity.showProgress(activity.getString(R.string.recognising))
        var result: String? = null
        GlobalScope.launch {
            result = startOCR(fileUri)
        }.invokeOnCompletion {
            activity.hideProgress()
            if (result != null) {
                activity.pushFragment(ResultFragment().also {
                    it.arguments = Bundle().also { args -> args.putString(ResultFragment.ARG_OCR_RESULT, result) }
                })
            }
        }
    }

    private fun checkDefaultTessdata() {
        try {
            val activity = activity as MainActivity? ?: return
            val assets = activity.assets ?: return
            val fileList = assets.list(TESSDATA) ?: return

            val tessDataDir = activity.getTesseractDataFolder()
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
        val lang = appSettings.getSelectedLanguage()
        if (lang == "eng")
            checkDefaultTessdata()

        tessBaseApi.init(path, lang)

        //banned special symbols
        tessBaseApi.setVariable(
                TessBaseAPI.VAR_CHAR_BLACKLIST,
                "×⦂⁃‐‑‒�–⎯—―~⁓•°%‰‱&⅋§÷±‼¡¿⸮⁇⁉⁈‽⸘¼½¾²³⅕⅙⅛©®™℠℻℅℁⅍¶⁋≠√�∛∜∞βΦΣ♀♂⚢⚣⌘♲♻☺★↑↓"
        )

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

    /*private fun Bitmap.rotate(degrees: Float): Bitmap =
        Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)*/

}