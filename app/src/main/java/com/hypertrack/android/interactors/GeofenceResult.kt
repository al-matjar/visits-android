package com.hypertrack.android.interactors

import com.hypertrack.android.models.local.Geofence

sealed class GeofenceResult
class GeofenceError(val e: Exception) : GeofenceResult()
class GeofenceSuccess(val geofence: Geofence) : GeofenceResult()
