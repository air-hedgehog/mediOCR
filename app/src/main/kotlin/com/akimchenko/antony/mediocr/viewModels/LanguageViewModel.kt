package com.akimchenko.antony.mediocr.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.akimchenko.anton.mediocr.R
import com.akimchenko.antony.mediocr.AppRepository
import com.akimchenko.antony.mediocr.adapters.ItemViewType
import com.akimchenko.antony.mediocr.adapters.LanguageItem
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.TESSDATA_SUFFIX
import com.akimchenko.antony.mediocr.utils.TESSDATA_URL
import org.jsoup.Jsoup

class LanguageViewModel(val repository: AppRepository) : ViewModel() {

    companion object {
        private const val CSS_QUERY = "td.content"
        private const val REFERENCE_CLASS_NAME = "td.content"
        private const val REFERENCE_ATTRIBUTE = "title"
    }

    private val _languageItems = MutableLiveData<List<LanguageItem>>()
    val languageItems: LiveData<List<LanguageItem>> = _languageItems

    private val _progressBarVisibility = MutableLiveData<Boolean>()
    val progressBarVisibility: LiveData<Boolean> = _progressBarVisibility

    private val _connectionError = MutableLiveData<Exception>()
    val connectionError: LiveData<Exception> = _connectionError


    private val downloadedLanguages = arrayListOf<LanguageItem>()
    private val availableLanguages = arrayListOf<LanguageItem>()

    private fun updateLanguages(isForce: Boolean, searchQuery: String?) {

        if (isForce) {
            downloadedLanguages.clear()
            availableLanguages.clear()
            _progressBarVisibility.value = true
            try {
                Jsoup.connect(TESSDATA_URL).timeout(6 * 1000).get()
                    .run {
                        this.select(CSS_QUERY).forEach { element ->
                            val reference =
                                element.getElementsByClass(REFERENCE_CLASS_NAME).attr(REFERENCE_ATTRIBUTE)

                            reference.endsWith(TESSDATA_SUFFIX)
                            val item = LanguageItem(
                                ItemViewType.ITEM_TYPE_LANGUAGE,
                                reference.removeSuffix(TESSDATA_SUFFIX)
                            )
                            availableLanguages.add(item)
                        }
                    }
            } catch (e: Exception) {
                _connectionError.value = e
            }
        }

        availableLanguages.filterTo(downloadedLanguages) { repository.isDownloaded(it) }
        //TODO check if filterTo removes item automatically
        availableLanguages.removeAll { downloadedLanguages.contains(it) }

        val rv = arrayListOf<LanguageItem>()
        if (downloadedLanguages.isNotEmpty()) {
            rv.add(
                LanguageItem(
                    ItemViewType.ITEM_TYPE_SEPARATOR,
                    repository.context.getString(R.string.downloaded)
                )
            )
            rv.addAll(downloadedLanguages.filterByQuery(searchQuery))
        }

        if (availableLanguages.isNotEmpty()) {
            rv.add(
                LanguageItem(
                    ItemViewType.ITEM_TYPE_SEPARATOR,
                    repository.context.getString(R.string.available)
                )
            )
            rv.addAll(downloadedLanguages.filterByQuery(searchQuery))
        }
        _progressBarVisibility.value = false
        _languageItems.value = rv
    }

    private fun List<LanguageItem>.filterByQuery(query: String?): List<LanguageItem> {
        return this.filter {
            query?.let { query ->
                it.title?.contains(
                    query
                )
            } ?: true
        }
    }

    fun download(item: LanguageItem) {
        if (!repository.isDownloaded(item)) {
            repository.downloadFile(item)
            AppSettings.setSelectedLanguage(item)
            updateLanguages(false, null)
        }
    }

    fun delete(item: LanguageItem) {
        repository.getFileForLanguage(item)?.takeIf { it.exists() }?.let {
            it.delete()
            updateLanguages(false, null)
        }

    }

}

