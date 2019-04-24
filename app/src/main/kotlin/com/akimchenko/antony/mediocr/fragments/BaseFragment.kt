package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import kotlinx.android.synthetic.main.fragment_recycler.*


abstract class BaseFragment: Fragment(), NotificationCenter.Observer {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        activity.setSupportActionBar(toolbar)
        if (toolbar?.navigationIcon != null)
            toolbar.setNavigationOnClickListener { activity.onBackPressed() }
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
}