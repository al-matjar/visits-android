package com.hypertrack.android.ui.common.select_destination

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.ui.common.util.Utils.isDoneAction
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.CurrentTripFragment
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_select_destination.*
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.FlowPreview

@FlowPreview
open class SelectDestinationFragment :
    ProgressDialogFragment(R.layout.fragment_select_destination) {

    protected open val vm: SelectDestinationViewModel by viewModels {
        Injector.provideUserScopeViewModelFactory()
    }

    private val adapter =
        GooglePlacesAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment).getMapAsync {
            vm.onMapReady(requireContext(), it)
        }

        toolbar.title = getString(R.string.select_destination)
        mainActivity().setSupportActionBar(toolbar)
        mainActivity().supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        locations.setLinearLayoutManager(requireContext())
        locations.adapter = adapter
        adapter.setOnItemClickListener { adapter, view, position ->
            vm.onPlaceItemClick(this.adapter.getItem(position))
        }

        val watcher = object : SimpleTextWatcher() {
            override fun afterChanged(text: String) {
                if (search.hasFocus()) {
                    vm.onSearchQueryChanged(text)
                }
                updateClearQueryView()
            }
        }
        search.addTextChangedListener(watcher)
        search.setOnClickListener {
            vm.onSearchQueryChanged(search.textString())
        }
        search.setOnEditorActionListener { v, actionId, event ->
            if (isDoneAction(actionId, event)) {
                Utils.hideKeyboard(requireActivity())
                true
            } else false
        }

        vm.loadingState.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            progressbar.setGoneState(!it)
        }

        vm.placesResults.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            adapter.clear()
            adapter.addAll(it)
            adapter.notifyDataSetChanged()
        }

        vm.showErrorMessageEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            SnackBarUtil.showErrorSnackBar(view, it)
        }

        vm.searchQuery.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            search.silentUpdate(watcher, it)
            search.setSelection(search.textString().length)
            updateClearQueryView()
        }

        vm.locationInfo.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            lLocationInfo.setGoneState(it == null)
            it?.let {
                tvLocationAddress.text = it.address
                tvPlaceName.text = it.placeName
                tvPlaceName.setGoneState(it.placeName == null)
            }
        }

        vm.showConfirmButton.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            confirm.setGoneState(!it)
        }

        vm.destination.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            findNavController().navigate(it)
        }

        vm.closeKeyboard.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            Utils.hideKeyboard(requireActivity())
        }

        vm.goBackEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                CurrentTripFragment.KEY_DESTINATION,
                it
            )
            findNavController().popBackStack()
            Utils.hideKeyboard(requireActivity())
        }

        vm.removeSearchFocusEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            search.clearFocus()
        }

        vm.showMyLocationButton.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            bMyLocation.setGoneState(!it)
        }

        bMyLocation.setOnClickListener {
            vm.onMyLocationClick()
        }

        destination_on_map.show()
        confirm.show()

        confirm.setOnClickListener {
            vm.onConfirmClicked()
        }

        bClear.setOnClickListener {
            search.setText("")
            updateClearQueryView()
        }

        vm.onViewCreated()
    }

    private fun updateClearQueryView() {
        bClear.setGoneState(search.textString().isBlank())
    }
}

@Parcelize
data class DestinationData(
    val latLng: LatLng,
    val address: String? = null,
    val name: String? = null
) : Parcelable
