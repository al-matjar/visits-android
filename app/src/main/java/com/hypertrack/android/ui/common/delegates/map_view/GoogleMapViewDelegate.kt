package com.hypertrack.android.ui.common.delegates.map_view

import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.hypertrack.android.interactors.app.AppErrorAction
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.onError
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseFragment
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.catchException
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.withEffects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking


@Suppress("EXPERIMENTAL_API_USAGE")
class GoogleMapViewDelegate(
    private val mapViewId: Int,
    fragment: BaseFragment<MainActivity>,
    private val appInteractor: AppInteractor,
    private val onMapReady: (GoogleMap) -> Unit
) : BaseFragment.FragmentDelegate<MainActivity>(fragment) {

    private var state: State = Initial()

    private fun handleAction(action: Action) {
        when (action) {
            is MapViewCreatedAction -> {
                when (val state = state) {
                    is Initial -> {
                        state.copy(mapView = action.mapView).withEffects()
                    }
                    is Attached -> {
                        withNonNullMapView(action) { mapView ->
                            if (state.map != null) {
                                mapReady(mapView, state.map)
                            } else {
                                state.copy(mapView = mapView).withEffects()
                            }
                        }
                    }
                    is MapReady -> {
                        Attached(
                            map = null,
                            action.mapView
                        ).withEffects()
                    }
                }
            }
            is MapReadyAction -> {
                when (val state = state) {
                    is Initial -> {
                        state.copy(map = action.map).withEffects()
                    }
                    is Attached -> {
                        if (state.mapView != null) {
                            mapReady(state.mapView, action.map)
                        } else {
                            state.copy(map = action.map).withEffects()
                        }
                    }
                    is MapReady -> {
                        mapReady(state.mapView, state.map)
                    }
                }
            }
            AttachedAction -> {
                when (val state = state) {
                    is Initial -> Attached(state.map, state.mapView).withEffects()
                    is Attached, is MapReady -> illegalAction(action, state)
                }
            }
            is OnDestroyAction -> {
                when (val state = state) {
                    is Initial -> state.mapView?.let { reduce(action, it) } ?: state.withEffects()
                    is Attached -> state.mapView?.let { reduce(action, it) } ?: state.withEffects()
                    is MapReady -> reduce(action, state.mapView)
                }
            }
            is OnLowMemoryAction -> {
                when (val state = state) {
                    is Initial -> {
                        state.mapView?.let { reduce(action, state, it) }
                            ?: state.withEffects()
                    }
                    is Attached -> {
                        state.mapView?.let { reduce(action, state, it) }
                            ?: state.withEffects()
                    }
                    is MapReady -> {
                        reduce(action, state, state.mapView)
                    }
                }
            }
            is OnPauseAction -> {
                when (val state = state) {
                    is Initial -> {
                        state.mapView?.let { reduce(action, state, it) }
                            ?: state.withEffects()
                    }
                    is Attached -> {
                        state.mapView?.let { reduce(action, state, it) }
                            ?: state.withEffects()
                    }
                    is MapReady -> {
                        reduce(action, state, state.mapView)
                    }
                }
            }
            is OnResumeAction -> {
                when (val state = state) {
                    is Initial -> {
                        state.mapView?.let { reduce(action, state, it) }
                            ?: state.withEffects()
                    }
                    is Attached -> {
                        state.mapView?.let { reduce(action, state, it) }
                            ?: state.withEffects()
                    }
                    is MapReady -> {
                        reduce(action, state, state.mapView)
                    }
                }
            }
            DetachedAction -> {
                when (val state = state) {
                    is Initial -> illegalAction(action, state)
                    is Attached -> Initial(state.map, state.mapView).withEffects()
                    is MapReady -> Initial(state.map, state.mapView).withEffects()
                }
            }
        }.also { result ->
            result.effects.forEach { applyEffect(it) }
            state = result.newState
        }
    }

    private fun reduce(
        action: OnDestroyAction,
        mapView: MapView
    ): ReducerResult<State, Flow<Unit>> {
        return state.withEffects({
            mapView.onDestroy()
        }.asFlow())
    }

    private fun reduce(
        action: OnLowMemoryAction,
        state: State,
        mapView: MapView
    ): ReducerResult<State, Flow<Unit>> {
        return state.withEffects({
            mapView.onLowMemory()
        }.asFlow())
    }

    private fun reduce(
        action: OnPauseAction,
        state: State,
        mapView: MapView
    ): ReducerResult<State, Flow<Unit>> {
        return state.withEffects({
            mapView.onPause()
        }.asFlow())
    }

    private fun reduce(
        action: OnResumeAction,
        state: State,
        mapView: MapView
    ): ReducerResult<State, Flow<Unit>> {
        return state.withEffects({
            mapView.onResume()
        }.asFlow())
    }

    private fun applyEffect(flow: Flow<Unit>) {
        runBlocking {
            flow.catchException {
                appInteractor.onError(it)
            }.collect()
        }
    }

    private fun mapReady(mapView: MapView, map: GoogleMap): ReducerResult<State, Flow<Unit>> {
        return MapReady(mapView, map).withEffects({
            onMapReady.invoke(map)
        }.asFlow())
    }

    private fun illegalAction(action: Action, state: State): ReducerResult<State, Flow<Unit>> {
        return state.withEffects({
            IllegalActionException(action, state).also {
                if (MyApplication.DEBUG_MODE) {
                    throw it
                } else {
                    appInteractor.onError(it)
                }
            }
            Unit
        }.asFlow())
    }

    private fun withNonNullMapView(
        action: MapViewCreatedAction,
        block: (MapView) -> ReducerResult<State, Flow<Unit>>
    ): ReducerResult<State, Flow<Unit>> {
        return if (action.mapView != null) {
            block.invoke(action.mapView)
        } else {
            state.withEffects({
                appInteractor.onError(NullPointerException("mapView"))
            }.asFlow())
        }
    }

    override fun onCreateView(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MapView?>(mapViewId).also {
            handleAction(MapViewCreatedAction(it))
        }?.apply {
            onCreate(savedInstanceState)
            getMapAsync {
                handleAction(MapReadyAction(it))
            }
        }
    }

    override fun onAttach(context: Context) {
        handleAction(AttachedAction)
    }

    override fun onDetach() {
        handleAction(DetachedAction)
    }

    override fun onResume() {
        handleAction(OnResumeAction)
    }

    override fun onPause() {
        handleAction(OnPauseAction)
    }

    override fun onDestroy() {
        handleAction(OnDestroyAction)
    }

    override fun onLowMemory() {
        handleAction(OnLowMemoryAction)
    }

}


