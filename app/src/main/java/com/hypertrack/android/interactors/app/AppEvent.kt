package com.hypertrack.android.interactors.app

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.app.state.HistoryState

sealed class AppEvent
data class UpdateUserLocationEvent(val userLocation: LatLng?) : AppEvent()
