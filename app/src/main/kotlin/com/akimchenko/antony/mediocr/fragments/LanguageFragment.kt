package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.adapters.LanguageDownloadAdapter
import kotlinx.android.synthetic.main.fragment_recycler.*


class LanguageFragment : BaseFragment() {

    private lateinit var adapter: LanguageDownloadAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_recycler, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        toolbar_title.text = activity.getString(R.string.download_languages)
        back_button.setOnClickListener { activity.onBackPressed() }
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
        val languages = activity.resources.getStringArray(R.array.tessdata_langs)
        adapter = LanguageDownloadAdapter(this, ArrayList<String>().also {
            it.add("eng")
            it.addAll(languages)
        })
        recycler_view.adapter = adapter
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