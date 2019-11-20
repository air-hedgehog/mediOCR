package com.akimchenko.antony.mediocr.utils

import android.content.Context
import com.akimchenko.antony.mediocr.R
import org.jsoup.Jsoup

object MyDownloadManager {

    @JvmStatic
    fun getInitialDownloadList(context: Context): ArrayList<String> {
        val rv = ArrayList<String>()
        Jsoup.connect(context.getString(R.string.tessdata_url)).timeout(6 * 1000).get().run {
            this.select("td.content").forEach { element ->
                val reference = element.getElementsByClass("js-navigation-open").attr("title")
                rv.add(reference)
            }
        }
        return rv
    }

}