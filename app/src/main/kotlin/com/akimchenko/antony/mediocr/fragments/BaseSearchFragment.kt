package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import kotlinx.android.synthetic.main.toolbar.*

abstract class BaseSearchFragment: BaseFragment(), SearchView.OnQueryTextListener {

    companion object {
        private const val SEARCH_INSTANCE_STATE = "search_instance_state"
    }

    private var searchQuery: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        searchQuery = savedInstanceState?.getString(SEARCH_INSTANCE_STATE, null)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        val activity = activity as MainActivity? ?: return
        menu ?: return
        inflater ?: return

        inflater.inflate(R.menu.search_menu, menu)
        val searchItem = menu.findItem(R.id.search_view)
        val searchView = searchItem.actionView as SearchView?
        searchView?.setOnQueryTextListener(this)

        val searchIcon: ImageView? = searchView?.findViewById(androidx.appcompat.R.id.search_button)
        searchIcon?.setColorFilter(ContextCompat.getColor(activity, R.color.colorAccent))
        if (!searchQuery.isNullOrBlank()) {
            searchItem.expandActionView()
            searchView?.setQuery(searchQuery, false)
            searchView?.isIconified = false
            searchView?.clearFocus()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val searchView: SearchView? = toolbar?.menu?.findItem(R.id.search_view)?.actionView as SearchView? ?: return
        outState.putString(SEARCH_INSTANCE_STATE, searchView?.query.toString())
    }
}