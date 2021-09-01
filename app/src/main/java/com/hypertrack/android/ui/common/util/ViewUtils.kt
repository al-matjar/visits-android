package com.hypertrack.android.ui.common.util

import android.content.Context
import android.graphics.Bitmap
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager

abstract class SimpleTextWatcher : TextWatcher {
    open fun afterChanged(text: String) {}

    override fun afterTextChanged(s: Editable?) {
        afterChanged((s ?: "").toString())
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}


abstract class SimplePageChangedListener : ViewPager.OnPageChangeListener {
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {}

    override fun onPageScrollStateChanged(state: Int) {}
}


fun EditText.silentUpdate(listener: TextWatcher, str: String?) {
    if (textString() != str) {
        removeTextChangedListener(listener)
        setText(str)
        addTextChangedListener(listener)
    }
}


fun List<View>.hide() {
    forEach {
        it.hide()
    }
}

fun List<View>.show() {
    forEach {
        it.show()
    }
}

fun RecyclerView.setLinearLayoutManager(context: Context) {
    layoutManager = LinearLayoutManager(context)
}

fun View.hide() {
    visibility = View.GONE
}

fun View.makeInvisible() {
    visibility = View.INVISIBLE
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.setInvisibleState(value: Boolean) {
    visibility = if (value) {
        View.INVISIBLE
    } else {
        View.VISIBLE
    }
}

fun View.setGoneState(value: Boolean): View {
    visibility = if (value) {
        View.GONE
    } else {
        View.VISIBLE
    }
    return this
}

fun View.goneIfNull(value: Any?) {
    visibility = if (value == null) {
        View.GONE
    } else {
        View.VISIBLE
    }
}

fun View.goneIfEmpty(value: List<Any>): View {
    visibility = if (value.isEmpty()) {
        View.GONE
    } else {
        View.VISIBLE
    }
    return this
}

fun View.showIfEmpty(value: List<Any>) {
    visibility = if (value.isNotEmpty()) {
        View.GONE
    } else {
        View.VISIBLE
    }
}

fun TextView.textString(): String {
    return text.toString()
}

fun String?.toView(textView: TextView) {
    textView.text = this
}

fun Int.toView(imageView: ImageView) {
    imageView.setImageResource(this)
}

fun Bitmap.toView(imageView: ImageView) {
    imageView.setImageBitmap(this)
}

fun Int?.toTextView(textView: TextView) {
    textView.text = this?.toString() ?: ""
}