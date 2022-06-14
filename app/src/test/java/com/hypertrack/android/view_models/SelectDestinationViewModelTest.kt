package com.hypertrack.android.view_models

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.MainCoroutineScopeRule
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.common.select_destination.reducer.*
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SelectDestinationViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @Test
    fun init() {
        val reducer = SelectDestinationViewModelReducer()

        runBlocking {
            var state: State = SelectDestinationViewModelReducer.INITIAL_STATE
            println(state)
            val userLocation = LatLng(37.4, -122.0)
            state = reducer.sendAction(
                state,
                UserLocationReceived(userLocation, "address")
            ).newState
            state = reducer.sendAction(
                state,
                MapReadyAction(mockk(relaxed = true), LatLng(0.0, 0.0), "address")
            ).newState
            state = reducer.sendAction(
                state,
                MapCameraMoved(LatLng(0.01, 0.0), "address", MovedByUser, true)
            ).newState
            state = reducer.sendAction(
                state,
                MapCameraMoved(
                    LatLng(
                        userLocation.latitude + 0.1,
                        userLocation.longitude + 0.1
                    ), "address", MovedByUser, false
                )
            ).newState

            state = reducer.sendAction(
                state,
                ConfirmClicked
            ).newState
        }
    }

    private fun SelectDestinationViewModelReducer.sendAction(
        oldState: State,
        action: Action
    ): ReducerResult {
        println("action = $action")
        val res = reduceAction(oldState, action)
        println("new state = ${res.newState}")
        println("effects = ${res.effects}")
        return res
    }

}
