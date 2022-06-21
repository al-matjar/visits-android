package com.hypertrack.android.ui.screens.visits_management.tabs.history

import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.hypertrack.android.models.local.Geotag
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.base.Consumable
import java.time.LocalDate

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class ViewEventEffect(
    val viewEventHandle: MutableLiveData<Consumable<ViewEvent>>,
    val viewEvent: ViewEvent
) : Effect()

data class OpenGeotagInfoDialogEffect(
    val viewEventHandle: MutableLiveData<Consumable<ViewEvent>>,
    val geotag: Geotag
) : Effect()

data class OpenGeofenceVisitInfoDialogEffect(
    val viewEventHandle: MutableLiveData<Consumable<ViewEvent>>,
    val visit: LocalGeofenceVisit
) : Effect()
