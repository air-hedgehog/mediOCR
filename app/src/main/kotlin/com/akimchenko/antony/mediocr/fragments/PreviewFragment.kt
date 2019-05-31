package com.akimchenko.antony.mediocr.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
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
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import com.edmodo.cropper.CropImageView
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.android.synthetic.main.fragment_preview.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class PreviewFragment : BaseFragment(), View.OnClickListener {

    companion object {
        const val TESSDATA = "tessdata"
        const val ARG_IMAGE_FILE_URI = "arg_image_file"
    }

    private var doWhenDownloaded: Runnable? = null
    private var tessBaseApi: TessBaseAPI? = null
    private var savingCroppedImageJob: Job? = null
    private var recognizingJob: Job? = null
    private lateinit var imageFile: File

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        val uriString = arguments?.getString(ARG_IMAGE_FILE_URI) ?: return
        val uri: Uri = Uri.parse(uriString)
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
        recognise_button.background = Utils.makeSelector(
            activity,
            ContextCompat.getDrawable(activity, R.drawable.square_button_bg)!!.toBitmap()
        )

        language_button.setOnClickListener(this)
        close_button.setOnClickListener(this)
        rotate_left_button.setOnClickListener(this)
        rotate_right_button.setOnClickListener(this)
        recognise_button.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val activity = activity as MainActivity? ?: return
        when (v) {
            close_button -> activity.popFragment(MainFragment::class.java.name)
            rotate_left_button -> crop_image_view.rotateImage(-90)
            rotate_right_button -> crop_image_view.rotateImage(90)
            language_button -> activity.pushFragment(LanguageFragment())
            recognise_button -> {
                if (isRecognitionStarted()) {
                    cancelRecognition()
                } else {
                    val runnable = Runnable {
                        updateProgressVisibility(true)
                        //TODO refactor to asyncTask due to 'GlobalScope.broadcast()' and 'GlobalScope.produce()' are experimental
                        savingCroppedImageJob = GlobalScope.launch {
                            if (imageFile.exists())
                                imageFile.delete()
                            imageFile.createNewFile()
                            Utils.writeBitmapToFile(crop_image_view.croppedImage, imageFile)
                        }
                        savingCroppedImageJob?.invokeOnCompletion {
                            if (savingCroppedImageJob != null && !savingCroppedImageJob!!.isCancelled) {
                                recognise(imageFile.toUri())
                                savingCroppedImageJob = null
                            } else {
                                updateProgressVisibility(false)
                            }
                        }
                    }
                    if (activity.downloadIdsLangs.containsValue(AppSettings.getSelectedLanguage()))
                        doWhenDownloaded = runnable
                    else
                        runnable.run()
                }

                updateProgressVisibility(true)
            }
        }
    }

    private fun cancelRecognition() {
        doWhenDownloaded = null
        tessBaseApi?.stop()
        savingCroppedImageJob?.cancel()
        savingCroppedImageJob = null
        recognizingJob?.cancel()
        recognizingJob = null
    }

    private fun updateRecognizeButton() {
        val activity = activity as MainActivity? ?: return
        val background = ContextCompat.getDrawable(activity, R.drawable.square_button_bg) ?: return
        background.setColorFilter(
            ContextCompat.getColor(
                activity,
                if (isRecognitionStarted()) R.color.red else R.color.colorAccent
            ), PorterDuff.Mode.SRC_ATOP
        )
        recognise_button?.background = Utils.makeSelector(activity, background.toBitmap())
    }

    override fun onPause() {
        super.onPause()
        updateProgressVisibility(false)
    }

    private fun isRecognitionStarted(): Boolean =
        (savingCroppedImageJob != null && savingCroppedImageJob!!.isActive && !savingCroppedImageJob!!.isCancelled) ||
                (recognizingJob != null && recognizingJob!!.isActive && !recognizingJob!!.isCancelled)

    private fun updateProgressVisibility(isVisible: Boolean) {
        activity?.runOnUiThread {
            updateRecognizeButton()
            progress_bar?.visibility = if (isVisible) View.VISIBLE else View.GONE
        }
    }

    private fun isLangDownloaded(activity: MainActivity): Boolean = activity.getTesseractDataFolder().listFiles()
        .contains(File(activity.getTesseractDataFolder(), "${AppSettings.getSelectedLanguage()}.traineddata"))

    override fun onNotification(id: Int, `object`: Any?) {
        super.onNotification(id, `object`)
        when (id) {
            NotificationCenter.LANG_DOWNLOADED -> {
                if ((`object` as String) == AppSettings.getSelectedLanguage()) {
                    val activity = activity as MainActivity? ?: return
                    updateProgressVisibility(!isLangDownloaded(activity))
                    doWhenDownloaded?.run()
                    doWhenDownloaded = null
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tessBaseApi?.end()
    }

    private fun recognise(fileUri: Uri) {
        val activity = activity as MainActivity? ?: return
        var result: String? = null
        recognizingJob = GlobalScope.launch {
            result = getHOCRString(fileUri)
        }
        recognizingJob?.invokeOnCompletion {
            if (recognizingJob != null && !recognizingJob!!.isCancelled) {
                if (result != null) {

                    val text = Jsoup.parse(result).wholeText()
                    activity.pushFragment(ResultFragment().also {
                        it.arguments = Bundle().also { args -> args.putString(ResultFragment.ARG_OCR_RESULT, text) }
                    })
                }
            }
            updateProgressVisibility(false)
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

    private fun getHOCRString(fileUri: Uri): String? {
        try {
            val options = BitmapFactory.Options()
            options.inSampleSize = 1
            val bitmap = BitmapFactory.decodeFile(fileUri.path, options)
            return extractText(bitmap)
        } catch (e: Exception) {
            Log.e(this::class.java.name, e.message)
        }
        return null
    }

    private fun extractText(bitmap: Bitmap): String? {
        val activity = activity as MainActivity? ?: return null
        tessBaseApi = TessBaseAPI(TessBaseAPI.ProgressNotifier { progressValues ->
            //TODO
            /*it.currentRect
            it.currentWordRect*/

            //showProgress("${activity.getString(R.string.recognising)} ${progressValues.percent}%")
        })
        tessBaseApi ?: return null
        //tessBaseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN)
        val path: String? = Utils.getInternalDirs(activity)[0]?.path ?: return null
        val lang = AppSettings.getSelectedLanguage()
        if (lang == "eng")
            checkDefaultTessdata()

        tessBaseApi!!.init(path, lang)

        //banned special symbols
        tessBaseApi!!.setVariable(
            TessBaseAPI.VAR_CHAR_BLACKLIST,
            "№×⦂‒�–⎯—―~⁓•°%‰‱&⅋§÷‼¡¿⸮⁇⁉⁈‽⸘¼½¾²³⅕⅙⅛©®™℠℻℅℁⅍¶⁋≠√�∛∜∞βΦΣ♀♂⚢⚣⌘♲♻☺★↑↓"
        )

        Log.d(this::class.java.name, "Training file loaded")
        tessBaseApi!!.setImage(bitmap)
        var extractedText: String? = null
        try {
            //TODO parse HOCR string
            extractedText = tessBaseApi?.getHOCRText(0)
        } catch (e: Exception) {
            Log.e(this::class.java.name, "Error in recognizing text.", e)
        }

        return extractedText
    }

}