package com.akimchenko.antony.mediocr.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.akimchenko.antony.mediocr.BuildConfig
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.AppSettings
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
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


class PreviewFragment(override val layoutResId: Int = R.layout.fragment_preview) : BaseFragment(),
    View.OnClickListener {

    companion object {
        const val TESSDATA = "tessdata"
        const val ARG_IMAGE_FILE_URI = "arg_image_file"

        @JvmStatic
        fun getBottomSheetItemBackground(context: Context, color: Int, topToBottom: Boolean): GradientDrawable {
            val colors = intArrayOf(ContextCompat.getColor(context, R.color.colorPrimary), color)
            return GradientDrawable(
                if (topToBottom) GradientDrawable.Orientation.TOP_BOTTOM else
                    GradientDrawable.Orientation.BOTTOM_TOP, colors
            ).apply {
                this.cornerRadius = context.resources.getDimension(R.dimen.bottom_sheet_element_radius)
            }
        }
    }

    private var tessBaseApi: TessBaseAPI? = null
    private var savingCroppedImageJob: Job? = null
    private var recognizingJob: Job? = null
    private var imageFile: File? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var visibleLanguagesList: List<FrameLayout>
    private lateinit var rectanglesSlotsList: Array<FrameLayout>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        val uriString = arguments?.getString(ARG_IMAGE_FILE_URI) ?: return
        val uri: Uri = Uri.parse(uriString)
        when {
            uri.scheme == "file" -> uri.path?.let { imageFile = File(it) }
            uri.scheme == "content" -> {
                val inputStream = activity.contentResolver.openInputStream(uri) ?: return
                imageFile = activity.getFileForBitmap()
                val outputStream = FileOutputStream(imageFile!!)
                val buffer = ByteArray(1024)

                while ((inputStream.read(buffer)) > 0)
                    outputStream.write(buffer)

                inputStream.close()
                outputStream.close()
            }
            else -> return
        }

        visibleLanguagesList = listOf<FrameLayout>(
            first_lang_slot,
            second_lang_slot,
            third_lang_slot
        )

        rectanglesSlotsList = arrayOf(
            first_rect_slot,
            second_rect_slot,
            third_rect_slot
        )

        visibleLanguagesList.forEach {
            it.setOnClickListener(this@PreviewFragment)
        }

        rectanglesSlotsList.forEach {
            it.setOnClickListener(this@PreviewFragment)
        }

        if (imageFile != null)
            cropper_view.setImageBitmap(BitmapFactory.decodeFile(imageFile?.absolutePath))
        else
            Toast.makeText(activity, activity.getString(R.string.error_reading_image), Toast.LENGTH_LONG).show()

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

    override fun onResume() {
        super.onResume()
        checkAvailableTessData()
        activity?.let { activity ->
            updateChosenLangs(activity)
            updateRectangles(activity)
        }
    }

    override fun onClick(v: View?) {
        val activity = activity as MainActivity? ?: return
        when (v) {
            close_button -> {
                activity.popFragment(MainFragment::class.java.name)
            }
            rotate_left_button -> cropper_view.setImageBitmap(cropper_view.drawable.toBitmap().rotate(-90.0f))
            rotate_right_button -> cropper_view.setImageBitmap(cropper_view.drawable.toBitmap().rotate(90.0f))

            language_layout,
            align_layout -> bottomSheetBehavior.state =
                if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED)
                    BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED

            recognise_button -> {
                if (isRecognitionStarted()) {
                    cancelRecognition()
                } else {

                    if (isSelectedLangsDownloaded()) {
                        var croppedBitmap: Bitmap? = null
                        //updateProgressVisibility(true)
                        //TODO refactor to asyncTask due to 'GlobalScope.broadcast()' and 'GlobalScope.produce()' are experimental
                        savingCroppedImageJob = GlobalScope.launch {
                            /*if (imageFile.exists())
                                imageFile.delete()
                            imageFile.createNewFile()*/

                            val cropRectangle = cropper_view.getRectanglesList()[0]!!
                            val matrixValues = FloatArray(9)
                            cropper_view.imageMatrix.getValues(matrixValues)
                            val originalBitmap = cropper_view.drawable.toBitmap()
                            val scaleX = matrixValues[Matrix.MSCALE_X]
                            val scaleY = matrixValues[Matrix.MSCALE_Y]
                            val bitmapLeft = matrixValues[Matrix.MTRANS_X].absoluteValue
                            val bitmapTop = matrixValues[Matrix.MTRANS_Y].absoluteValue
                            println("originalbitmap height and width : ${originalBitmap.height} ${originalBitmap.width}")

                            val cropX = ((cropRectangle.getX() - bitmapLeft) / scaleX)
                                .coerceIn(bitmapLeft, bitmapLeft + originalBitmap.width / scaleX)
                            val cropY = ((cropRectangle.getY() - bitmapTop) / scaleY)
                                .coerceIn(bitmapTop, bitmapTop + originalBitmap.height / scaleY)

                            val cropWidth = (cropRectangle.getRectangle().width() / scaleX)
                            val cropHeight = (cropRectangle.getRectangle().height() / scaleY)


                            println("crop x y : $cropX $cropY")
                            println("bitmap width and height : ${originalBitmap.width} ${originalBitmap.height}")
                            println("bitmap left and top : $bitmapLeft $bitmapTop")

                            croppedBitmap = Bitmap.createBitmap(originalBitmap, cropX.roundToInt(),
                                cropY.roundToInt(), cropWidth.roundToInt(), cropHeight.roundToInt())
                            //Utils.writeBitmapToFile(croppedBitmap!!, imageFile)
                        }
                        savingCroppedImageJob?.invokeOnCompletion {
                            activity.runOnUiThread {
                                cropper_view.setImageBitmap(croppedBitmap)
                            }
                        }

                        /* savingCroppedImageJob?.invokeOnCompletion {
                            if (savingCroppedImageJob != null && !savingCroppedImageJob!!.isCancelled) {
                                recognise(imageFile.toUri())
                                savingCroppedImageJob = null
                            } else {
                                //updateProgressVisibility(false)
                            }
                        }*/

                        //TODO fix logic
                        /*cropper x y : 364 607
                        bitmap width and height : 504 506
                        cropper width and height : 345 355
                        x + width must be <= bitmap.width()*/

                    } else {
                        AlertDialog.Builder(activity).setMessage(R.string.languages_not_downloaded)
                            .setPositiveButton(R.string.download_languages) { dialog, _ ->
                                (activity as MainActivity?)?.let { activity ->
                                    AppSettings.getSelectedLanguageList().forEach { lang ->
                                        Utils.download(
                                            activity,
                                            lang,
                                            File(activity.getTesseractDataFolder(), "$lang.traineddata")
                                        )
                                    }
                                }
                                dialog.dismiss()
                            }.setNegativeButton(R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }.create().show()
                    }
                }
                //TODO progress alertDialog
            }

            first_lang_slot,
            second_lang_slot,
            third_lang_slot -> {
                val slotNumber = when (v) {
                    first_lang_slot -> 0
                    second_lang_slot -> 1
                    else -> 2
                }

                val clickedLang = try {
                    AppSettings.getSelectedLanguageList()[slotNumber]
                } catch (e: IndexOutOfBoundsException) {
                    null
                }

                if (clickedLang == null) {
                    showLanguageFragment(activity, slotNumber)
                } else {
                    PopupMenu(activity, v).also { popup ->
                        popup.menu.add(0, 0, 0, getString(R.string.remove))
                        popup.menu.add(0, 1, 1, getString(R.string.replace))
                        popup.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                0 -> {
                                    AppSettings.removeSelectedLanguage(AppSettings.getSelectedLanguageList()[slotNumber])
                                    updateChosenLangs(activity)
                                }
                                1 -> showLanguageFragment(activity, slotNumber)
                            }
                            false
                        }
                        popup.show()
                    }
                }
            }
            first_rect_slot,
            second_rect_slot,
            third_rect_slot -> {
                v ?: return

                val rectSlotIndex = when (v) {
                    first_rect_slot -> 0
                    second_rect_slot -> 1
                    else -> 2
                }

                val clickedRect = cropper_view.getRectanglesList()[rectSlotIndex]

                if (clickedRect == null) {
                    cropper_view.addRectangle(rectSlotIndex)
                } else {
                    //TODO popup with 'remove' and 'replace' actions
                    cropper_view.removeRectangle(rectSlotIndex)
                }
                updateRectangles(activity)
            }
        }
    }

    private fun showLanguageFragment(activity: MainActivity, languageIndex: Int) {
        LanguageFragment().also { fragment ->
            fragment.arguments = Bundle().also { args ->
                args.putInt(LanguageFragment.LANGUAGE_INDEX_ARG, languageIndex)
            }
            activity.pushFragment(fragment)
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

    private fun isRecognitionStarted(): Boolean =
        (savingCroppedImageJob != null && savingCroppedImageJob!!.isActive && !savingCroppedImageJob!!.isCancelled) ||
                (recognizingJob != null && recognizingJob!!.isActive && !recognizingJob!!.isCancelled)

    private fun isSelectedLangsDownloaded(): Boolean {
        val activity = activity as MainActivity? ?: return false
        val downloadedLangFiles = activity.getTesseractDataFolder().listFiles()
        for (lang in AppSettings.getSelectedLanguageList()) {
            if (lang == "eng") continue

            if (downloadedLangFiles.isNullOrEmpty()) return false

            if (downloadedLangFiles.contains(
                    File(activity.getTesseractDataFolder(), "$lang.traineddata"))) return false

            if (activity.downloadIdsLangs.containsValue(lang)) return false
        }
        return true
    }

    override fun onNotification(id: Int, `object`: Any?) {
        super.onNotification(id, `object`)
        when (id) {
            /*NotificationCenter.LANG_DOWNLOADED -> {
                if (AppSettings.getSelectedLanguageList().contains((`object` as String)))
                    //updateProgressVisibility(!isSelectedLangsDownloaded())
            }*/
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
            checkAvailableTessData()
            result = getHOCRString(fileUri)
        }
        recognizingJob?.invokeOnCompletion {
            if (recognizingJob != null && !recognizingJob!!.isCancelled && result != null) {
                if (BuildConfig.DEBUG)
                    Log.d(PreviewFragment::class.java.name, "OCR_result:\n$result")

                val text = Jsoup.parse(result).wholeText()
                activity.pushFragment(ResultFragment().also {
                    it.arguments = Bundle().also { args ->
                        args.putString(
                            ResultFragment.ARG_OCR_RESULT,
                            text
                        )
                    }
                })
            }
            //updateProgressVisibility(false)
        }
    }

    private fun checkAvailableTessData() {
        AppSettings.getSelectedLanguageList().also { langs ->
            if (langs.isNullOrEmpty() || langs.contains("eng")) {
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
                    e.message?.let { Log.e(this::class.java.name, it) }
                }
                if (langs.isNullOrEmpty())
                    AppSettings.addSelectedLanguage("eng")
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun updateChosenLangs(context: Context) {
        val list = AppSettings.getSelectedLanguageList()
        val inflater = LayoutInflater.from(context)
        for (i in visibleLanguagesList.indices) {
            visibleLanguagesList[i].removeAllViews()
            try {
                val lang = list[i]
                val langView = inflater.inflate(R.layout.item_bottom_sheet_language, null, false) as ViewGroup
                langView.background = getBottomSheetItemBackground(
                    context,
                    ContextCompat.getColor(context, R.color.colorPrimaryDark),
                    true
                )
                (langView.findViewById<TextView>(R.id.text_view)).text = Utils.getLocalizedLangName(lang)
                visibleLanguagesList[i].addView(langView)
            } catch (e: IndexOutOfBoundsException) {
                visibleLanguagesList[i].addView(getEmptyItem(context))
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun getEmptyItem(context: Context): View = LayoutInflater.from(context).inflate(
        R.layout.item_bottom_sheet_empty_item, null, false
    )
        .apply {
            this.background = getBottomSheetItemBackground(
                context,
                ContextCompat.getColor(context, R.color.colorPrimaryDark), false
            )
        }

    private fun updateRectangles(context: Context) {
        val rects = cropper_view.getRectanglesList()

        for (i in rectanglesSlotsList.indices) {
            rectanglesSlotsList[i].removeAllViews()

            val rect = rects[i]
            if (rect != null) {

                val gradientRect = FrameLayout(context).apply {
                    this.layoutParams =
                        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    this.background = getBottomSheetItemBackground(context, rect.getColor(), true)
                }

                rectanglesSlotsList[i].addView(gradientRect)
            } else {
                rectanglesSlotsList[i].addView(getEmptyItem(context))
            }
        }
    }

    private fun getHOCRString(fileUri: Uri): String? {
        try {
            val options = BitmapFactory.Options()
            options.inSampleSize = 1
            val bitmap = BitmapFactory.decodeFile(fileUri.path, options)
            return extractText(bitmap)
        } catch (e: Exception) {
            e.message?.let { Log.e(this::class.java.name, it) }
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

        val lang = AppSettings.getSelectedLanguageList().joinToString("+")

        tessBaseApi!!.init(path, lang)
        tessBaseApi!!.initLanguagesAsString

        //banned special symbols
        tessBaseApi!!.setVariable(
            TessBaseAPI.VAR_CHAR_BLACKLIST,
            "⦂�⎯⁓&⅋§‽⸘¼½¾²³⅕⅙⅛©®™℠℻℅℁⅍¶⁋�∞♀♂⚢⚣⌘♲♻☺★"
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