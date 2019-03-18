package com.akimchenko.antony.mediocr.fragments

import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.utils.NotificationCenter

abstract class BaseFragment: Fragment(), NotificationCenter.Observer {

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