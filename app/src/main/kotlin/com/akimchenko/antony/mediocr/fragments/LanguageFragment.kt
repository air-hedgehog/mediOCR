package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.adapters.LanguageDownloadAdapter
import kotlinx.android.synthetic.main.fragment_recycler.*
import kotlinx.android.synthetic.main.toolbar.*


class LanguageFragment : BaseSearchFragment() {

    private lateinit var adapter: LanguageDownloadAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_recycler, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        toolbar.title = activity.getString(R.string.download_languages)
        toolbar.navigationIcon = ContextCompat.getDrawable(activity, R.drawable.arrow_back)
        toolbar.setNavigationOnClickListener { activity.onBackPressed() }
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
        adapter = LanguageDownloadAdapter(activity)
        recycler_view.adapter = adapter
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