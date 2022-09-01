package com.hypertrack.android.ui.screens.add_integration

import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.models.Integration

sealed class Effect
data class UpdateViewStateEffect(val state: State, val viewState: ViewState) : Effect()
data class OnIntegrationSelectedEffect(val integration: Integration) : Effect()
data class LoadIntegrationsEffect(val query: String, val pageToken: String?) : Effect()
data class AppEffect(val appEffect: AppEffect) : Effect()
