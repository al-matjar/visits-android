package com.hypertrack.android.ui.screens.add_place_info

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.SupportMapFragment
import com.hypertrack.android.di.Injector
import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.utils.stringFromResource
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_add_place_info.*
import kotlinx.android.synthetic.main.fragment_add_place_info.confirm
import kotlinx.android.synthetic.main.fragment_add_place_info.toolbar
import kotlinx.android.synthetic.main.inflate_error.bReload
import kotlinx.android.synthetic.main.inflate_error.tvErrorMessage
import kotlinx.android.synthetic.main.inflate_integration.*

class AddPlaceInfoFragment : BaseFragment<MainActivity>(R.layout.fragment_add_place_info) {

    private val args: AddPlaceInfoFragmentArgs by navArgs()
    private val vm: AddPlaceInfoViewModel by viewModels {
        Injector.provideUserScopeParamViewModelFactory(
            args.destinationData
        )
    }

    private var enableGeofenceNameTouchListener = false
    private val geofenceNameListener = object : SimpleTextWatcher() {
        override fun afterChanged(text: String) {
            vm.onGeofenceNameChanged(text)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Integration>(KEY_INTEGRATION)
            ?.observe(viewLifecycleOwner) { result ->
                result?.let {
                    vm.onIntegrationAdded(it)
                    findNavController().currentBackStackEntry?.savedStateHandle
                        ?.set(KEY_INTEGRATION, null)
                }
            }

        toolbar.title = getString(R.string.add_place)
        mainActivity().setSupportActionBar(toolbar)
        mainActivity().supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        (childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment).getMapAsync {
            vm.onMapReady(requireContext(), it)
        }

        val addressListener = object : SimpleTextWatcher() {
            override fun afterChanged(text: String) {
                vm.onAddressChanged(text)
            }
        }
        etAddress.addTextChangedListener(addressListener)

        val radiusListener = object : SimpleTextWatcher() {
            override fun afterChanged(text: String) {
                vm.onRadiusChanged(text)
            }
        }
        etRadius.addTextChangedListener(radiusListener)

        etGeofenceName.addTextChangedListener(geofenceNameListener)

        vm.viewState.observe(viewLifecycleOwner) { viewState ->
            try {
                if (viewState.errorMessage == null) {
                    lError.hide()
                    confirm.show()

                    lProgressBar.setGoneState(!viewState.isLoading)
                    confirm.setGoneState(viewState.isLoading)

                    etAddress.silentUpdate(addressListener, viewState.address)
                    etRadius.silentUpdate(radiusListener, viewState.radius.orEmpty())
                    displayIntegrationFieldState(viewState.integrationsViewState)
                    confirm.isSelected = viewState.enableConfirmButton
                } else {
                    lError.show()
                    lProgressBar.hide()
                    confirm.hide()
                    bReload.hide()
                    tvErrorMessage.text = viewState.errorMessage.text
                }
            } catch (e: Exception) {
                vm.handleAction(ErrorAction(e))
            }
        }

        vm.errorHandler.errorText.observe(viewLifecycleOwner) {
            it.consume {
                SnackbarUtil.showErrorSnackbar(view, it)
            }
        }

        vm.destination.observe(viewLifecycleOwner) {
            findNavController().navigate(it)
        }

        vm.adjacentGeofenceDialogEvent.observe(viewLifecycleOwner) {
            it.consume {
                createConfirmationDialog(it).show()
            }
        }

        etGeofenceName.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (enableGeofenceNameTouchListener) {
                    vm.onAddIntegration()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        bAddIntegration.setOnClickListener {
            vm.onAddIntegration()
        }

        confirm.setOnClickListener {
            vm.onConfirmClicked(
                GeofenceCreationParams(
                    name = etGeofenceName.textString(),
                    address = etAddress.textString(),
                    description = etGeofenceDescription.textString()
                )
            )
        }

        bDeleteIntegration.setOnClickListener {
            vm.onDeleteIntegrationClicked()
        }
    }

    private fun displayIntegrationFieldState(integrationsView: IntegrationsViewState) {
        when (integrationsView) {
            is HasIntegrations -> {
                enableGeofenceNameTouchListener = true
                when (integrationsView.integrationFieldState) {
                    is ShowGeofenceName -> {
                        listOf(etGeofenceName, tvGeofenceName).forEach {
                            it.show()
                        }
                        tvGeofenceName.setText(
                            integrationsView.integrationFieldState.geofenceNameHint
                        )
                        etGeofenceName.silentUpdate(geofenceNameListener, null)
                        lIntegration.hide()
                    }
                    is ShowIntegration -> {
                        listOf(etGeofenceName, tvGeofenceName).forEach {
                            it.hide()
                        }
                        lIntegration.show()

                        val integration = integrationsView.integrationFieldState.integration
                        integration.name?.toView(tvIntegrationName)
                        integration.id.toView(tvIntegrationId)
                        listOf(tvIntegrationType, tvIntegrationTypeHint).forEach {
                            it.hide()
                        }
                    }
                }
            }
            is NoIntegrations -> {
                enableGeofenceNameTouchListener = false
                listOf(etGeofenceName, tvGeofenceName).forEach {
                    it.show()
                }
                tvGeofenceName.setText(integrationsView.geofenceNameHint)
                etGeofenceName.silentUpdate(geofenceNameListener, integrationsView.geofenceName)
                lIntegration.hide()
            }
        }
    }

    private fun createConfirmationDialog(params: GeofenceCreationParams): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setMessage(
                R.string.add_place_confirm_adjacent.stringFromResource()
            )
            .setPositiveButton(R.string.yes) { dialog, which ->
                vm.onGeofenceDialogYes(params)
            }
            .setNegativeButton(R.string.no) { _, _ ->
            }
            .create()
    }

    companion object {
        const val KEY_INTEGRATION = "integration"
    }
}
