package com.hypertrack.android.ui.screens.place_details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.interactors.app.state.PlaceDetailsScreen
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.adapters.KeyValueAdapter
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_place_details.*
import kotlinx.android.synthetic.main.fragment_place_details.lIntegration
import kotlinx.android.synthetic.main.inflate_integration.*
import kotlinx.android.synthetic.main.inflate_integration.view.bCopy
import kotlinx.android.synthetic.main.inflate_integration.view.tvIntegrationId
import kotlinx.android.synthetic.main.inflate_integration_details.view.*

class PlaceDetailsFragment : ProgressDialogFragment(R.layout.fragment_place_details) {

    private val args: PlaceDetailsFragmentArgs by navArgs()
    private val vm: PlaceDetailsViewModel by viewModels {
        Injector.provideUserScopeParamViewModelFactory(
            args.geofenceId
        )
    }
    private lateinit var map: GoogleMap

    private val metadataAdapter = KeyValueAdapter(true)
    private val visitsAdapter by lazy { vm.createVisitsAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Injector.provideAppInteractor().handleAction(RegisterScreenAction(PlaceDetailsScreen))

        (childFragmentManager.findFragmentById(R.id.liveMap) as SupportMapFragment).getMapAsync {
            vm.onMapReady(it)
        }

        rvMetadata.setLinearLayoutManager(requireContext())
        rvMetadata.adapter = metadataAdapter
        metadataAdapter.onCopyClickListener = {
            vm.onCopyValue(it)
        }

        rvVisits.setLinearLayoutManager(requireContext())
        rvVisits.adapter = visitsAdapter

        vm.loadingState.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            srlPlaces.isRefreshing = it
        }

        vm.address.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            tvAddress.text = it
        }

        vm.metadata.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            metadataAdapter.updateItems(it)
        }

        vm.integration.observeWithErrorHandling(
            viewLifecycleOwner,
            vm::onError
        ) { integrationValue ->
            lIntegration.setGoneState(integrationValue == null)
            integrationValue?.let { integration ->
                integration.id.toView(tvIntegrationId)
                integration.name?.toView(tvIntegrationName)
                listOf(tvIntegrationType, tvIntegrationTypeHint).forEach {
                    it.hide()
                }
            }
        }

        vm.visits.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            visitsAdapter.updateItems(it)
            tvNoVisits.setGoneState(it.isNotEmpty())
        }

        vm.externalMapsIntent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            it.consume {
                mainActivity().startActivity(it)
            }
        }

        vm.showErrorMessageEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            SnackBarUtil.showErrorSnackBar(view, it)
        }

//        srlPlaces.setOnRefreshListener {
//            vm.onRefresh()
//        }
        srlPlaces.isEnabled = false

        ivBack.setOnClickListener {
            mainActivity().onBackPressed()
        }

        bDirections.setOnClickListener {
            vm.onDirectionsClick()
        }

        lAddress.setOnClickListener {
            vm.onAddressClick()
        }

        lIntegration.bCopy.setOnClickListener {
            vm.onCopyIntegrationName()
        }

        lIntegration.let { listOf(it.bCopyId, it.tvIntegrationId) }.forEach {
            it.setOnClickListener {
                vm.onCopyIntegrationId()
            }
        }
    }

}

