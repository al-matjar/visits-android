package com.hypertrack.android.use_case.app

import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.reducer.geofences_for_map.GeofencesForMapReducer
import com.hypertrack.android.use_case.geofences.CheckForAdjacentGeofencesUseCase
import com.hypertrack.android.use_case.geofences.LoadGeofencesForMapUseCase
import com.hypertrack.android.utils.Intersect

class UserScopeUseCases(
    private val appInteractor: AppInteractor,
    private val appScope: AppScope,
    private val userScope: UserScope
) {

    val loadGeofencesForMapUseCase = LoadGeofencesForMapUseCase(userScope.placesRepository)

    val checkForAdjacentGeofencesUseCase = CheckForAdjacentGeofencesUseCase(
        appInteractor,
        GeofencesForMapReducer(),
        Intersect()
    )

    override fun toString(): String = javaClass.simpleName
}
