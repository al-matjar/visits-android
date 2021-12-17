package com.hypertrack.android.view_models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.MainCoroutineScopeRule
import com.hypertrack.android.observeAndAssertNull
import com.hypertrack.android.observeAndGetValue
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoFragment
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoFragmentDirections
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoViewModel
import com.hypertrack.android.utils.ResultSuccess
import com.hypertrack.android.utils.ResultValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
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
    fun `it should show add integration screen on place name click if integrations enabled`() {
        runBlocking {
            val integrationsRepository: IntegrationsRepository = mockk(relaxed = true) {
                coEvery { hasIntegrations() } returns ResultSuccess(true)
            }
            AddPlaceInfoViewModel(
                LatLng(0.0, 0.0),
                "",
                "",
                mockk(relaxed = true),
                mockk(relaxed = true),
                integrationsRepository,
                mockk(relaxed = true),
                mockk(relaxed = true),
            ).let {
                it.onAddIntegration()

                runBlocking {
                    assertEquals(
                        AddPlaceInfoFragmentDirections.actionAddPlaceInfoFragmentToAddIntegrationFragment(),
                        it.destination.observeAndGetValue().payload
                    )
                    coVerify {
                        integrationsRepository.hasIntegrations()
                    }
                }
            }
        }
    }

    @Test
    fun `it should not show add integration screen on place name click if integrations disabled`() {
        runBlocking {
            val integrationsRepository: IntegrationsRepository = mockk(relaxed = true) {
                coEvery { hasIntegrations() } returns ResultSuccess(false)
            }
            AddPlaceInfoViewModel(
                LatLng(0.0, 0.0),
                "",
                "",
                mockk(relaxed = true),
                mockk(relaxed = true),
                integrationsRepository,
                mockk(relaxed = true),
                mockk(relaxed = true),
            ).let {
                it.onAddIntegration()

                runBlocking {
                    it.destination.observeAndAssertNull()
                    coVerify {
                        integrationsRepository.hasIntegrations()
                    }
                }
            }
        }
    }

}
