package com.akimchenko.antony.mediocr.fragments

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.akimchenko.antony.mediocr.BuildConfig
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.android.synthetic.main.bottom_sheet_fill.*
import kotlinx.android.synthetic.main.bottom_sheet_header.*
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

    private var tessBaseApi: TessBaseAPI? = null
    private var savingCroppedImageJob: Job? = null
    private var recognizingJob: Job? = null
    private lateinit var imageFile: File
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

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
        recognise_button.background = Utils.makeSelector(
                activity,
                ContextCompat.getDrawable(activity, R.drawable.square_button_bg)!!.toBitmap()
        )

        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById<View>(R.id.bottom_sheet)).apply {
            this.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(p0: View, p1: Float) {

                }

                override fun onStateChanged(p0: View, p1: Int) {

                }
            })
        }

        language_layout.setOnClickListener(this)
        close_button.setOnClickListener(this)
        rotate_left_button.setOnClickListener(this)
        rotate_right_button.setOnClickListener(this)
        recognise_button.setOnClickListener(this)
        align_layout.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            close_button -> {
                val activity = activity as MainActivity? ?: return
                activity.popFragment(MainFragment::class.java.name)
            }
            rotate_left_button -> image_view.setImageBitmap(image_view.drawable.toBitmap().rotate(-90.0f))
            rotate_right_button -> image_view.setImageBitmap(image_view.drawable.toBitmap().rotate(90.0f))

            language_layout,
            align_layout -> bottomSheetBehavior.state = if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED)
                BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED

            recognise_button -> {
                if (isRecognitionStarted()) {
                    cancelRecognition()
                } else {

                    if (isSelectedLangsDownloaded()) {
                        updateProgressVisibility(true)
                        //TODO refactor to asyncTask due to 'GlobalScope.broadcast()' and 'GlobalScope.produce()' are experimental
                        savingCroppedImageJob = GlobalScope.launch {
                            if (imageFile.exists())
                                imageFile.delete()
                            imageFile.createNewFile()
                            //TODO own custom cropper
                            //Utils.writeBitmapToFile(crop_image_view.croppedImage, imageFile)
                            Utils.writeBitmapToFile(image_view.drawable.toBitmap(), imageFile)
                        }
                        savingCroppedImageJob?.invokeOnCompletion {
                            if (savingCroppedImageJob != null && !savingCroppedImageJob!!.isCancelled) {
                                recognise(imageFile.toUri())
                                savingCroppedImageJob = null
                            } else {
                                updateProgressVisibility(false)
                            }
                        }
                    } else {
                        AlertDialog.Builder(activity).setMessage(R.string.languages_not_downloaded)
                                .setPositiveButton(R.string.download_languages) { dialog, _ ->
                                    (activity as MainActivity?)?.let { activity ->
                                        AppSettings.getSelectedLanguageList().forEach { lang ->
                                            Utils.download(activity, lang, File(activity.getTesseractDataFolder(), "$lang.traineddata"))
                                        }
                                    }
                                    dialog.dismiss()
                                }.setNegativeButton(R.string.cancel) { dialog, _ ->
                                    dialog.dismiss()
                                }.create().show()
                    }
                }

                updateProgressVisibility(true)
            }

            first_lang_slot,
            second_lang_slot,
            third_lang_slot -> {
                val langNumber = when (v) {
                    first_lang_slot -> 0
                    second_lang_slot -> 1
                    else -> 2
                }

                //TODO alertDialog with non-selected languages or LanguageFragment with onClick allowance attribute
            }
        }
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap =
            Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)

    private fun cancelRecognition() {
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
            progress_bar?.visibility = if (isVisible) View.VISIBLE else View.GONE
            updateRecognizeButton()
        }
    }

    private fun isSelectedLangsDownloaded(): Boolean {
        val activity = activity as MainActivity? ?: return false
        for (lang in AppSettings.getSelectedLanguageList()) {
            if (lang != "eng" && !activity.getTesseractDataFolder().listFiles()
                            .contains(File(activity.getTesseractDataFolder(), "$lang.traineddata")) ||
                    activity.downloadIdsLangs.containsValue(lang)) {
                return false
            }
        }
        return true
    }

    override fun onNotification(id: Int, `object`: Any?) {
        super.onNotification(id, `object`)
        when (id) {
            NotificationCenter.LANG_DOWNLOADED -> {
                if (AppSettings.getSelectedLanguageList().contains((`object` as String)))
                    updateProgressVisibility(!isSelectedLangsDownloaded())
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
                    if (BuildConfig.DEBUG)
                        Log.d(PreviewFragment::class.java.name, "OCR_result:\n$result")

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
        tessBaseApi!!.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT
        val path: String? = Utils.getInternalDirs(activity)[0]?.path ?: return null

        if (AppSettings.getSelectedLanguageList().contains("eng"))
            checkDefaultTessdata()

        val lang = AppSettings.getSelectedLanguageList().joinToString("+")

        tessBaseApi!!.init(path, lang)
        tessBaseApi!!.initLanguagesAsString

        //banned special symbols
        tessBaseApi!!.setVariable(
                TessBaseAPI.VAR_CHAR_BLACKLIST,
                "№×⦂‒�–⎯—―~⁓•°%‰‱&⅋§÷‼¡¿⸮⁇⁉⁈‽⸘¼½¾²³⅕⅙⅛©®™℠℻℅℁⅍¶⁋≠√�∛∜∞βΦΣ♀♂⚢⚣⌘♲♻☺★↑↓"
        )

        Log.d(this::class.java.name, "Training file loaded")
        tessBaseApi!!.setImage(bitmap)
        var extractedText: String? = null
        try {
            extractedText = tessBaseApi?.getHOCRText(0)
        } catch (e: Exception) {
            Log.e(this::class.java.name, "Error in recognizing text.", e)
        }

        return extractedText
    }

}