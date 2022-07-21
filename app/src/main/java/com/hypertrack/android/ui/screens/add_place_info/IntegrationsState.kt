package com.hypertrack.android.ui.screens.add_place_info

import com.hypertrack.android.models.Integration

sealed class IntegrationsState {
    override fun toString(): String = javaClass.simpleName
}

data class IntegrationsDisabled(val geofenceName: String?) : IntegrationsState()
data class IntegrationsEnabled(val integration: Integration?) : IntegrationsState()
