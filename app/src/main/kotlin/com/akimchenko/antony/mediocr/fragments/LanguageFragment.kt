package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.akimchenko.anton.mediocr.R
import com.akimchenko.anton.mediocr.databinding.FragmentRecyclerBinding
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.adapters.LanguageDownloadAdapter
import com.akimchenko.antony.mediocr.viewModels.LanguageViewModel
import org.koin.android.ext.android.inject


class LanguageFragment : BaseSearchFragment<FragmentRecyclerBinding>() {

    private val viewModel: LanguageViewModel by inject()
    override val searchView: SearchView?
        get() = binding.toolbar.root.menu?.findItem(R.id.search_view)?.actionView as SearchView?
    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentRecyclerBinding {
        return FragmentRecyclerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        val adapter: LanguageDownloadAdapter
        binding.run {
            toolbar.root.run {
                title = activity.getString(R.string.download_languages)
                navigationIcon = AppCompatResources.getDrawable(activity, R.drawable.arrow_back)
                setNavigationOnClickListener { onBackPressed() }
            }

            recyclerView.run {
                layoutManager = LinearLayoutManager(activity)
                addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
                adapter = LanguageDownloadAdapter()
                this.adapter = adapter
            }
        }
        viewModel.languageItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        viewModel.progressBarVisibility.observe(viewLifecycleOwner) {
            binding.progressBar.root.isVisible = it
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

}