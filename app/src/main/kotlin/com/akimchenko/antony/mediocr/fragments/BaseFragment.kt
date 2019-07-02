package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import kotlinx.android.synthetic.main.toolbar.*


abstract class BaseFragment: Fragment(), NotificationCenter.Observer {

    abstract val layoutResId: Int

    companion object {
        const val ITEM_SETTINGS = 0
        const val ITEM_SORT_TYPE_DATE = 1
        const val ITEM_SORT_TYPE_TITLE = 2
        const val ITEM_SEARCH = 3
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(layoutResId, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        activity.setSupportActionBar(toolbar)
    }

    override fun onResume() {
        super.onResume()
        NotificationCenter.addObserver(this)
    }

    override fun onPause() {
        super.onPause()
        NotificationCenter.removeObserver(this)
    }

    override fun onNotification(id: Int, `object`: Any?) {
        //catch notification where it need
    }

    open fun onBackPressed() {
        activity?.supportFragmentManager?.popBackStack()
    }
}