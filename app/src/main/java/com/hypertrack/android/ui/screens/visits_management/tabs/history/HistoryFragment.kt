package com.hypertrack.android.ui.screens.visits_management.tabs.history

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.math.MathUtils
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseFragment
import com.hypertrack.android.ui.common.util.SnackbarUtil
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_history.bAddGeotag
import kotlinx.android.synthetic.main.fragment_history.mapLoaderCanvas
import kotlinx.android.synthetic.main.fragment_history.progress
import kotlinx.android.synthetic.main.fragment_history.rvTimeline
import kotlinx.android.synthetic.main.fragment_history.scrim
import kotlinx.android.synthetic.main.fragment_tracking.destination
import kotlinx.android.synthetic.main.progress_bar.*

class HistoryFragment : BaseFragment<MainActivity>(R.layout.fragment_history) {

    private val vm: HistoryViewModel by viewModels {
        MyApplication.injector.provideUserScopeViewModelFactory()
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync {
            vm.onMapReady(requireContext(), it)
        }

        vm.loadingState.observe(viewLifecycleOwner, {
            displayLoadingState(it)
        })

        //value doesn't represent correct state
        vm.bottomSheetOpened.observe(viewLifecycleOwner, {
            bottomSheetBehavior.state = if (it) {
                BottomSheetBehavior.STATE_EXPANDED
            } else {
                BottomSheetBehavior.STATE_COLLAPSED
            }
            rvTimeline.scrollToPosition(0)
        })

        vm.tiles.observe(viewLifecycleOwner) {
            rvTimeline.adapter?.notifyDataSetChanged()
        }

        vm.errorHandler.errorText.observe(viewLifecycleOwner, {
            SnackbarUtil.showErrorSnackbar(view, it)
        })

        vm.destination.observe(viewLifecycleOwner) { consumable ->
            consumable.consume {
                findNavController().navigate(it)
            }
        }

        setupTimeline()

        bAddGeotag.setOnClickListener {
            vm.onAddGeotagClick()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    private fun setupTimeline() {
        bottomSheetBehavior = BottomSheetBehavior.from(rvTimeline)
        bottomSheetBehavior.peekHeight = vm.style.summaryPeekHeight

        val adapter = TimelineTileItemAdapter(
            vm.tiles,
            vm.style
        ) { vm.onTileSelected(it) }
        rvTimeline.adapter = adapter
        rvTimeline.layoutManager = LinearLayoutManager(MyApplication.context)

        scrim.setOnClickListener { bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val baseColor = Color.BLACK
                val baseAlpha =
                    ResourcesCompat.getFloat(resources, R.dimen.material_emphasis_medium)
                val alpha = MathUtils.lerp(0f, 255f, slideOffset * baseAlpha).toInt()
                val color = Color.argb(alpha, baseColor.red, baseColor.green, baseColor.blue)
                scrim.setBackgroundColor(color)
                scrim.visibility = if (slideOffset > 0) View.VISIBLE else View.GONE
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }
        })
    }

    private fun displayLoadingState(isLoading: Boolean) {
        progress?.setGoneState(!isLoading)
        mapLoaderCanvas?.setGoneState(!isLoading)
        progress?.background = null
        if (isLoading) loader?.playAnimation() else loader?.cancelAnimation()
    }
}

