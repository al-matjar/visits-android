package com.hypertrack.android.ui.screens.visits_management

import androidx.annotation.ColorRes
import androidx.annotation.StringRes

data class TrackingIndicatorState(
    @ColorRes val color: Int,
    @StringRes val statusMessageResource: Int,
    @StringRes val trackingMessageResource: Int,
    val isTracking: Boolean
)
