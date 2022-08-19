package com.hypertrack.android.ui.common.select_destination

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GooglePlacesSearchDelegate(
    private val crashReportsProvider: CrashReportsProvider,
    private val googlePlacesInteractor: GooglePlacesInteractor
) {

    private var token: AutocompleteSessionToken? = null

    suspend fun search(query: String, location: LatLng?): Result<List<GooglePlaceModel>> {
        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling selectPlace()).
        if (token == null) {
            token = googlePlacesInteractor.createSessionToken()
        }

        return token?.let {
            googlePlacesInteractor.getPlaces(query, it, location)
        } ?: listOf<GooglePlaceModel>().asSuccess().also {
            crashReportsProvider.logException(NullPointerException("Google Places token == null"))
        }
    }

    suspend fun fetchPlace(item: GooglePlaceModel): Result<Place> {
        return token?.let {
            googlePlacesInteractor.fetchPlace(item, it).also {
                token = null
            }
        } ?: Failure<Place>(NullPointerException("Google Places token == null")).also {
            crashReportsProvider.logException(it.exception)
        }
    }

}
