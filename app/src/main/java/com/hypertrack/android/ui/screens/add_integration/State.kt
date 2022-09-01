package com.hypertrack.android.ui.screens.add_integration

import com.hypertrack.android.models.Integration

sealed class State {
    fun getIntegrationsIfPresent(): List<Integration> {
        return when (this) {
            is Loaded -> integrations
            is Loading -> listOf()
        }
    }
}

data class Loading(val query: String) : State()
data class Loaded(
    val query: String,
    val integrations: List<Integration>,
    val nextPageToken: String?,
    val newQuery: String
) : State()
