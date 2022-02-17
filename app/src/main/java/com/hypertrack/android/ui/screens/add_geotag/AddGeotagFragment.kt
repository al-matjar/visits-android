package com.hypertrack.android.ui.screens.add_geotag

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.SupportMapFragment
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseFragment
import com.hypertrack.android.ui.common.adapters.EditableKeyValueAdapter
import com.hypertrack.android.ui.common.util.SnackbarUtil
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.setLinearLayoutManager
import com.hypertrack.android.ui.common.util.toViewOrHideIfNull
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_add_geotag.bAddField
import kotlinx.android.synthetic.main.fragment_add_geotag.bCreateGeotag
import kotlinx.android.synthetic.main.fragment_add_geotag.map
import kotlinx.android.synthetic.main.fragment_add_geotag.rvGeotagMetadata
import kotlinx.android.synthetic.main.fragment_add_geotag.tvAddGeotagError
import kotlinx.android.synthetic.main.fragment_add_geotag.tvAddGeotagHint
import kotlinx.android.synthetic.main.fragment_select_destination.toolbar
import kotlinx.coroutines.FlowPreview

@FlowPreview
class AddGeotagFragment : BaseFragment<MainActivity>(R.layout.fragment_add_geotag) {

    private val vm: AddGeotagViewModel by viewModels {
        Injector.provideUserScopeViewModelFactory()
    }

    private val metadataAdapter = EditableKeyValueAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync {
            vm.onMapReady(it)
        }

        toolbar.title = getString(R.string.add_geotag)
        mainActivity().setSupportActionBar(toolbar)
        mainActivity().supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        rvGeotagMetadata.setLinearLayoutManager(requireContext())
        rvGeotagMetadata.adapter = metadataAdapter

        vm.viewState.observe(viewLifecycleOwner) { viewState ->
            viewState.errorText.toViewOrHideIfNull(tvAddGeotagError)
            tvAddGeotagHint.setGoneState(!viewState.showHint)
            listOf(rvGeotagMetadata, bAddField).forEach {
                it.setGoneState(!viewState.showMetadata)
            }
            viewState.createButtonEnabled.let { enabled ->
                bCreateGeotag.isSelected = enabled
                bCreateGeotag.isEnabled = enabled
            }
            map.setGoneState(!viewState.showMap)
        }

        vm.popBackStack.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }

        vm.snackbar.observe(viewLifecycleOwner) {
            SnackbarUtil.showErrorSnackbar(view, it)
        }

        bAddField.setOnClickListener {
            metadataAdapter.addNewField()
        }

        bCreateGeotag.setOnClickListener {
            vm.onCreateClick(metadataAdapter.items)
        }
    }
}
