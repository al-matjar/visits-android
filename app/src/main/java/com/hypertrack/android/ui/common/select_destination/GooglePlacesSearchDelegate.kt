package com.hypertrack.android.ui.common.select_destination

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.utils.Result
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GooglePlacesSearchDelegate(
    private val googlePlacesInteractor: GooglePlacesInteractor
) {

    private var token: AutocompleteSessionToken? = null

    suspend fun search(query: String, location: LatLng?): Result<List<GooglePlaceModel>> {
        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling selectPlace()).
        if (token == null) {
            token = googlePlacesInteractor.createSessionToken()
        }

        return googlePlacesInteractor.getPlaces(query, token!!, location)
    }

    suspend fun fetchPlace(item: GooglePlaceModel): Result<Place> {
        return googlePlacesInteractor.fetchPlace(item, token!!).also {
            token = null
        }
    }


}
