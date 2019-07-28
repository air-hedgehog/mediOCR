package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.adapters.LanguageDownloadAdapter
import com.akimchenko.antony.mediocr.utils.AppSettings
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import kotlinx.android.synthetic.main.fragment_recycler.*
import kotlinx.android.synthetic.main.toolbar_progress_bar.*
import kotlinx.android.synthetic.main.toolbar.*


class LanguageFragment(override val layoutResId: Int = R.layout.fragment_recycler) : BaseSearchFragment() {

    companion object {
        const val LANGUAGE_INDEX_ARG = "language_index_arg"
    }

    private lateinit var adapter: LanguageDownloadAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        toolbar.title = activity.getString(R.string.languages)
        toolbar.navigationIcon = ContextCompat.getDrawable(activity, R.drawable.arrow_back)
        toolbar.setNavigationOnClickListener { activity.onBackPressed() }
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
        adapter = LanguageDownloadAdapter(this)
        recycler_view.adapter = adapter
    }

    fun updateProgressBar(isVisible: Boolean) {
        progress_bar?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun onNotification(id: Int, `object`: Any?) {
        when (id) {
            NotificationCenter.LANG_DELETED -> {
                if (`object` == AppSettings.getSelectedLanguageList().contains(`object` as String)) {
                    AppSettings.removeSelectedLanguage(`object`)
                }
                adapter.updateItems()
            }
            NotificationCenter.LANGUAGE_REPLACED,
            NotificationCenter.LANG_DOWNLOADED -> adapter.updateItems()
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        adapter.setQuery(newText)
        return false
    }

    override fun onResume() {
        super.onResume()
        adapter.resume()
    }

    override fun onPause() {
        super.onPause()
        adapter.pause()
    }
}