package com.hypertrack.android.ui.screens.place_details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_place_details.*
import kotlinx.android.synthetic.main.fragment_place_details.lIntegration
import kotlinx.android.synthetic.main.inflate_integration.*
import kotlinx.android.synthetic.main.inflate_integration.view.*

class PlaceDetailsFragment : ProgressDialogFragment(R.layout.fragment_place_details) {

    private val args: PlaceDetailsFragmentArgs by navArgs()
    private val vm: PlaceDetailsViewModel by viewModels {
        MyApplication.injector.provideParamVmFactory(
            args.geofenceId
        )
    }
    private lateinit var map: GoogleMap

    private val metadataAdapter = KeyValueAdapter(true)
    private val visitsAdapter by lazy { vm.createVisitsAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (childFragmentManager.findFragmentById(R.id.liveMap) as SupportMapFragment?)?.getMapAsync {
            vm.onMapReady(it)
        }

        rvMetadata.setLinearLayoutManager(requireContext())
        rvMetadata.adapter = metadataAdapter
        metadataAdapter.onCopyClickListener = {
            vm.onCopyValue(it)
        }

        rvVisits.setLinearLayoutManager(requireContext())
        rvVisits.adapter = visitsAdapter

        lIntegration.bDeleteIntegration.hide()
        lIntegration.bCopy.show()

        vm.loadingState.observe(viewLifecycleOwner, {
            srlPlaces.isRefreshing = it
        })

        vm.address.observe(viewLifecycleOwner, {
            tvAddress.text = it
        })

        vm.metadata.observe(viewLifecycleOwner, {
            metadataAdapter.updateItems(it)
        })

        vm.integration.observe(viewLifecycleOwner, {
            lIntegration.setGoneState(it == null)
            it?.let {
                it.id.toView(tvIntegrationId)
                it.name?.toView(tvIntegrationName)
//                it.type.toView(tvIntegrationType)
                listOf(tvIntegrationType, tvIntegrationTypeHint).forEach {
                    it.hide()
                }
            }
        })

        vm.visits.observe(viewLifecycleOwner, {
            visitsAdapter.updateItems(it)
            tvNoVisits.setGoneState(it.isNotEmpty())
        })

        vm.externalMapsIntent.observe(viewLifecycleOwner, {
            it.consume {
                mainActivity().startActivity(it)
            }
        })

        vm.errorHandler.errorText.observe(viewLifecycleOwner, {
            SnackbarUtil.showErrorSnackbar(view, it)
        })

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
            vm.onIntegrationCopy()
        }
    }

}

