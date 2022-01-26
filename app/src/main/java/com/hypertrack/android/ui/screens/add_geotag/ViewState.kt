package com.hypertrack.android.ui.screens.add_geotag

import com.hypertrack.android.utils.ErrorMessage

sealed class ViewState(
    val showHint: Boolean,
    val showMetadata: Boolean,
    val showMap: Boolean,
    val createButtonEnabled: Boolean,
    val errorText: String?
) {
    override fun toString(): String = javaClass.simpleName
}

object InitialViewState : ViewState(
    createButtonEnabled = false,
    showHint = true,
    showMetadata = false,
    showMap = true,
    errorText = null
)

class GeotagCreationViewState : ViewState(
    createButtonEnabled = true,
    showHint = true,
    showMetadata = true,
    showMap = true,
    errorText = null
)

class ErrorViewState(errorMessage: ErrorMessage) : ViewState(
    createButtonEnabled = false,
    showHint = false,
    showMetadata = false,
    showMap = false,
    errorText = errorMessage.toString()
)
