package com.hypertrack.android.di

import com.hypertrack.android.ui.common.select_destination.DestinationData

/**
 * trip creation scope is needed for the case when there will be several screens
 * that will add the information for new trip
 */
class TripCreationScope(
    val destinationData: DestinationData
)
