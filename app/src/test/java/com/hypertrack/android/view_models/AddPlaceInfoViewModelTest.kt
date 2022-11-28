package com.hypertrack.android.view_models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.hypertrack.android.MainCoroutineScopeRule
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoEffectsHandler
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoReducer
import com.hypertrack.android.ui.screens.add_place_info.Effect
import com.hypertrack.android.ui.screens.add_place_info.GeofenceCreationParams
import com.hypertrack.android.ui.screens.add_place_info.GeofenceNameClickedAction
import com.hypertrack.android.ui.screens.add_place_info.Initialized
import com.hypertrack.android.ui.screens.add_place_info.IntegrationsDisabled
import com.hypertrack.android.ui.screens.add_place_info.IntegrationsEnabled
import com.hypertrack.android.ui.screens.add_place_info.OpenAddIntegrationScreenEffect
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class AddPlaceInfoViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `it should show add integration screen on place name click if integrations enabled and don't show if not enabled`() {
        val reducer = AddPlaceInfoReducer(
            mockk(),
            mapUiReducer = mockk()
        )
        val mockLiveData = mockk<MutableLiveData<Consumable<NavDirections>>>(relaxed = true)
        val effectsHandler = AddPlaceInfoEffectsHandler(
            appInteractor = mockk(),
            placeLocation = mockk(),
            init = mockk(),
            handleAction = mockk(),
            viewState = mockk(),
            adjacentGeofenceDialogEvent = mockk(),
            placesInteractor = mockk(),
            mapItemsFactory = mockk(),
            mapUiEffectHandler = mockk(),
            getErrorMessageUseCase = mockk(),
            showErrorUseCase = mockk(),
            destination = mockLiveData,
            geofencesForMapStateFlow = mockk(),
            logExceptionToCrashlyticsUseCase = mockk(),
        )

        val userState = userLoggedIn()
        createInitializedState(integrationsEnabled = true).let { state ->
            reducer.reduce(GeofenceNameClickedAction, state, userState).let {
                assertEquals(state, it.newState)
                assertEquals(setOf<Effect>(OpenAddIntegrationScreenEffect), it.effects)
                runBlocking {
                    it.effects.forEach { effectsHandler.getEffectFlow(it).collect() }
                }
            }
            verify { mockLiveData.postValue(any()) }
        }

        createInitializedState(integrationsEnabled = false).let { state ->
            reducer.reduce(GeofenceNameClickedAction, state, userState).let {
                assertEquals(state, it.newState)
                assertEquals(setOf<Effect>(), it.effects)
            }
            verify { mockLiveData.postValue(any()) }
        }
    }

    companion object {
        fun createInitializedState(
            integrationsEnabled: Boolean
        ): Initialized {
            return Initialized(
                mapUiState = mockk(),
                location = mockk(),
                integrations = if (integrationsEnabled) {
                    IntegrationsEnabled(null)
                } else {
                    IntegrationsDisabled(null)
                },
                address = null,
                radius = null,
            )
        }
    }

}
