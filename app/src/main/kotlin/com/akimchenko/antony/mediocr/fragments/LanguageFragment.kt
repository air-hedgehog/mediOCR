package com.akimchenko.antony.mediocr.fragments

import android.app.SearchManager
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.widget.SearchView
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
        toolbar.title = activity.getString(R.string.download_languages)
        setHasOptionsMenu(true)
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))

        adapter = LanguageDownloadAdapter(activity)
        recycler_view.adapter = adapter

    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu ?: return
        inflater ?: return

        inflater.inflate(R.menu.search_menu, menu)
        val searchItem = menu.findItem(R.id.search_view)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
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