package com.akimchenko.antony.mediocr

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.akimchenko.anton.mediocr.R
import com.akimchenko.antony.mediocr.adapters.LanguageItem
import com.akimchenko.antony.mediocr.utils.PREINSTALLED_LANGUAGE
import com.akimchenko.antony.mediocr.utils.TESSDATA_SUFFIX
import com.akimchenko.antony.mediocr.utils.TESSDATA_URL
import com.akimchenko.antony.mediocr.utils.Utils
import java.io.File

class AppRepository(val context: Context) {

    private val _languageDownloaded = MutableLiveData<LanguageItem>()
    val languageDownloaded: LiveData<LanguageItem> = _languageDownloaded

    val ongoingDownloadLanguages: HashMap<Long, LanguageItem> = HashMap()
    fun getInternalFiles(): List<File> = context.fileList().map { File(it) }

    fun createFile(fileName: String): File {
        return File(context.filesDir, fileName)
    }

    fun notifyLanguageDownloaded(downloadManagerId: Long) {
        _languageDownloaded.value = ongoingDownloadLanguages[downloadManagerId]
        ongoingDownloadLanguages.remove(downloadManagerId)
    }

    fun writeToFile(file: File, content: String) {
        context.openFileOutput(file.name, Context.MODE_PRIVATE).use {
            it.write(content.toByteArray())
        }
    }

    fun getFileForLanguage(item: LanguageItem): File? {
        return getInternalFiles().find { it.name == "${item.title}$TESSDATA_SUFFIX"}
    }

    fun downloadFile(item: LanguageItem) {
        val request =
            DownloadManager.Request(Uri.parse("$TESSDATA_URL${item.title}$TESSDATA_SUFFIX"))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setTitle(Utils.getLocalizedLangName(item.title!!))
                .setDestinationUri(Uri.fromFile(createFile("${item.title}$TESSDATA_SUFFIX")))
                .setAllowedOverRoaming(true)
                .setAllowedOverMetered(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            request.setRequiresCharging(false)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
            ?: return
        ongoingDownloadLanguages[downloadManager.enqueue(request)] = item
    }

    fun isDownloaded(languageItem: LanguageItem): Boolean {
        return preinstalledLanguage(languageItem) ||  getFileForLanguage(languageItem) != null
    }

    fun preinstalledLanguage(languageItem: LanguageItem): Boolean {
        return languageItem.title != PREINSTALLED_LANGUAGE
    }

    fun getDefaultSavedFilesDirectory(): File {
        val savedFilesDirectory = File(context.filesDir, context.getString(R.string.saved_files))
        if (!savedFilesDirectory.exists() || !savedFilesDirectory.isDirectory)
            savedFilesDirectory.mkdirs()
        return savedFilesDirectory
    }

    fun getDefaultCroppedImagesDirectory(): File {
        val croppedImageDirectory = File(context.filesDir, context.getString(R.string.cropped_images))
        if (!croppedImageDirectory.exists() || !croppedImageDirectory.isDirectory)
            croppedImageDirectory.mkdirs()
        return croppedImageDirectory
    }
}