package com.hypertrack.android.ui.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class BaseFragment<T : Activity>(layoutId: Int) : Fragment(layoutId) {

    fun mainActivity(): T = activity as T

    open val delegates: List<FragmentDelegate<T>> by lazy {
        mutableListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState).also { view ->
            view?.let {
                delegates.forEach { it.onCreateView(view, savedInstanceState) }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        delegates.forEach { it.onAttach(context) }
    }

    override fun onDetach() {
        super.onDetach()
        delegates.forEach { it.onDetach() }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        delegates.forEach { it.onLowMemory() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        delegates.forEach { it.onViewCreated(view) }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        delegates.forEach { it.onDestroyView() }
    }

    override fun onResume() {
        super.onResume()
        delegates.forEach { it.onResume() }
    }

    override fun onPause() {
        super.onPause()
        delegates.forEach { it.onPause() }
    }

    override fun onStop() {
        super.onStop()
        delegates.forEach { it.onStop() }
    }

    override fun onDestroy() {
        super.onDestroy()
        delegates.forEach { it.onDestroy() }
    }

    open fun onLeave() {
        delegates.forEach { it.onLeave() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        delegates.forEach { it.onSaveState(outState) }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            delegates.forEach { it.onLoadState(savedInstanceState) }
        }
    }

    abstract class FragmentDelegate<M : Activity>(protected val fragment: BaseFragment<M>) {
        open fun onCreateView(
            view: View,
            savedInstanceState: Bundle?
        ) {
        }

        open fun onViewCreated(view: View) {}
        open fun onResume() {}
        open fun onDestroyView() {}
        open fun onPause() {}
        open fun onStop() {}
        open fun onDestroy() {}
        open fun onLeave() {}
        open fun onSaveState(bundle: Bundle) {}
        open fun onLoadState(bundle: Bundle) {}
        open fun onLowMemory() {}
        open fun onAttach(context: Context) {}
        open fun onDetach() {}
        open fun onBackPressed(): Boolean {
            return false
        }
    }

    open fun onBackPressed(): Boolean {
        if (isAdded) {
            delegates.forEach {
                if (it.onBackPressed()) {
                    return true
                }
            }
        }
        return false
    }

}
