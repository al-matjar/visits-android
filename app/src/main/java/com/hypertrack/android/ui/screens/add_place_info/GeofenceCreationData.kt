package com.hypertrack.android.ui.screens.add_place_info

import com.hypertrack.android.models.Integration

data class GeofenceCreationData(
    val params: GeofenceCreationParams,
    val radius: Int,
    val integration: Integration?
)
