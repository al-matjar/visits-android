package com.hypertrack.android.ui.screens.add_place_info

import androidx.annotation.StringRes
import com.hypertrack.android.models.Integration
import com.hypertrack.android.utils.ErrorMessage

data class ViewState(
    val radius: Int?,
    val address: String?,
    val enableConfirmButton: Boolean,
    val integrationsViewState: IntegrationsViewState,
    val isLoading: Boolean,
    val errorMessage: ErrorMessage?,
    val showRetryButton: Boolean
)

sealed class IntegrationsViewState
class NoIntegrations(
    @StringRes val geofenceNameHint: Int,
    val geofenceName: String?,
) : IntegrationsViewState()

data class HasIntegrations(
    val integrationFieldState: IntegrationFieldState
) : IntegrationsViewState()

sealed class IntegrationFieldState
data class ShowGeofenceName(
    @StringRes val geofenceNameHint: Int,
) : IntegrationFieldState()

data class ShowIntegration(val integration: Integration) : IntegrationFieldState()
