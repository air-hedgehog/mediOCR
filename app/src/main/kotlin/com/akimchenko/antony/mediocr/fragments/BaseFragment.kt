package com.akimchenko.antony.mediocr.fragments

import androidx.viewbinding.ViewBinding


abstract class BaseFragment<out T : ViewBinding> : BindingFragment<T>() {


    open fun onBackPressed() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }
}