package com.hypertrack.android.ui.common


data class DataPage<T>(
    val items: List<T>,
    val paginationToken: String?
)
