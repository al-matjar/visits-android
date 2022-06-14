package com.hypertrack.android.view_models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.MainCoroutineScopeRule
import com.hypertrack.android.observeAndAssertNull
import com.hypertrack.android.observeAndGetValue
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoEffectsHandler
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoFragment
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoFragmentDirections
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoReducer
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoViewModel
import com.hypertrack.android.ui.screens.add_place_info.Effect
import com.hypertrack.android.ui.screens.add_place_info.GeofenceNameClickedAction
import com.hypertrack.android.ui.screens.add_place_info.Initial
import com.hypertrack.android.ui.screens.add_place_info.Initialized
import com.hypertrack.android.ui.screens.add_place_info.IntegrationsDisabled
import com.hypertrack.android.ui.screens.add_place_info.IntegrationsEnabled
import com.hypertrack.android.ui.screens.add_place_info.OpenAddIntegrationScreenEffect
import com.hypertrack.android.utils.ResultSuccess
import com.hypertrack.android.utils.ResultValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        )
        val mockLiveData = mockk<MutableLiveData<Consumable<NavDirections>>>(relaxed = true)
        val effectsHandler = AddPlaceInfoEffectsHandler(
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockLiveData,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
        )

        createInitializedState(integrationsEnabled = true).let { state ->
            reducer.reduce(state, GeofenceNameClickedAction).let {
                assertEquals(state, it.newState)
                assertEquals(setOf<Effect>(OpenAddIntegrationScreenEffect), it.effects)
                runBlocking {
                    it.effects.forEach { effectsHandler.applyEffect(it) }
                }
            }
            verify { mockLiveData.postValue(any()) }
        }

        createInitializedState(integrationsEnabled = false).let { state ->
            reducer.reduce(state, GeofenceNameClickedAction).let {
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
                mockk(),
                if (integrationsEnabled) {
                    IntegrationsEnabled(null)
                } else {
                    IntegrationsDisabled(null)
                },
                null,
                null,
            )
        }
    }

}
