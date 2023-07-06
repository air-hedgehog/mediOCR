package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.akimchenko.anton.mediocr.R
import com.akimchenko.antony.mediocr.MainActivity

abstract class BaseSearchFragment<out T : ViewBinding> : BaseFragment<T>(),
    SearchView.OnQueryTextListener {

    companion object {
        private const val SEARCH_INSTANCE_STATE = "search_instance_state"
    }

    private var searchQuery: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        searchQuery = savedInstanceState?.getString(SEARCH_INSTANCE_STATE, null)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val activity = activity as MainActivity? ?: return

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

    protected abstract val searchView: SearchView?

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SEARCH_INSTANCE_STATE, searchView?.query?.toString())
    }
}