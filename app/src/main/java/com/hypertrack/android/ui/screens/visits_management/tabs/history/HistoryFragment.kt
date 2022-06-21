package com.hypertrack.android.ui.screens.visits_management.tabs.history

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import com.hypertrack.android.ui.common.util.SnackBarUtil
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_history.bAddGeotag
import kotlinx.android.synthetic.main.fragment_history.bSelectDate
import kotlinx.android.synthetic.main.fragment_history.mapLoaderCanvas
import kotlinx.android.synthetic.main.fragment_history.progress
import kotlinx.android.synthetic.main.fragment_history.scrim
import kotlinx.android.synthetic.main.progress_bar.*

import android.view.LayoutInflater
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.ui.common.util.toViewOrHideIfNull
import com.hypertrack.android.utils.createDatePickerDialog
import kotlinx.android.synthetic.main.fragment_history.lError
import kotlinx.android.synthetic.main.fragment_history.lTimeline
import kotlinx.android.synthetic.main.inflate_error.view.bReload
import kotlinx.android.synthetic.main.inflate_error.view.tvErrorMessage
import kotlinx.android.synthetic.main.inflate_timeline.ivTimelineArrowUp
import kotlinx.android.synthetic.main.inflate_timeline.lTimelineHeader
import kotlinx.android.synthetic.main.inflate_timeline.rvTimeline
import kotlinx.android.synthetic.main.inflate_timeline.tvSummaryDistance
import kotlinx.android.synthetic.main.inflate_timeline.tvSummaryDuration
import kotlinx.android.synthetic.main.inflate_timeline.tvSummaryTitle
import kotlinx.android.synthetic.main.inflate_timeline_dialog_gefence_visit.view.bCloseDialog
import kotlinx.android.synthetic.main.inflate_timeline_dialog_geotag.view.bCopyId
import kotlinx.android.synthetic.main.inflate_timeline_dialog_geotag.view.bCopyMetadata
import kotlinx.android.synthetic.main.inflate_timeline_dialog_geotag.view.tvGeotagAddress
import kotlinx.android.synthetic.main.inflate_timeline_dialog_geotag.view.tvGeotagId
import kotlinx.android.synthetic.main.inflate_timeline_dialog_geotag.view.tvGeotagMetadata
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.bCopy
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.ivRouteTo
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvDescription
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvPlaceAddress
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvPlaceIntegrationName
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvRouteTo
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvTitle
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvVisitId
import java.time.LocalDate


class HistoryFragment : BaseFragment<MainActivity>(R.layout.fragment_history) {

    private val vm: HistoryViewModel by viewModels {
        Injector.provideViewModelFactory()
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var timelineAdapter: TimelineTileItemAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            timelineAdapter = vm.createTimelineAdapter()

            (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync {
                vm.onMapReady(it)
            }

            vm.viewState.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { viewState ->
                displayLoadingState(viewState.showProgressbar)
                viewState.errorText.let {
                    lError.setGoneState(it == null)
                    lError.tvErrorMessage.text = it?.text
                }
                tvSummaryTitle.text = viewState.daySummaryTitle
                tvSummaryDistance.text = viewState.totalDriveDistanceText
                tvSummaryDuration.text = viewState.totalDriveDurationText
                tvSummaryDistance.setGoneState(viewState.totalDriveDistanceText == null)
                tvSummaryDuration.setGoneState(viewState.totalDriveDurationText == null)
                viewState.dateText.let {
                    bSelectDate.text = it
                    bSelectDate.setGoneState(it == null)
                }
                viewState.showUpArrow.let { show ->
                    if (!show) {
                        ivTimelineArrowUp.clearAnimation()
                    }
                    ivTimelineArrowUp.setGoneState(!show)
                }
                viewState.showAddGeotagButton.let { show ->
                    bAddGeotag.animation?.cancel()
                    bAddGeotag.apply {
                        if (show) {
                            show()
                        } else {
                            hide()
                        }
                    }
                }
                timelineAdapter.updateItems(viewState.tiles)
            }

            vm.viewEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { consumable ->
                consumable.consume { event ->
                    when (event) {
                        is SetBottomSheetStateEvent -> {
                            bottomSheetBehavior.state = if (event.expanded) {
                                BottomSheetBehavior.STATE_EXPANDED
                            } else {
                                BottomSheetBehavior.STATE_COLLAPSED
                            }
                            rvTimeline.scrollToPosition(0)

                            ivTimelineArrowUp.clearAnimation()
                            ivTimelineArrowUp.animate().apply {
                                if (event.arrowDown) {
                                    rotation(180f)
                                } else {
                                    rotation(0f)
                                }
                            }.setDuration(ANIMATION_DURATION).start()
                        }
                        is ShowDatePickerDialogEvent -> {
                            showDatePickerDialog(event.date)
                        }
                        is ShowGeofenceVisitDialogEvent -> {
                            createGeofenceVisitDialog(event.visitDialog).show()
                        }
                        is ShowGeotagDialogEvent -> {
                            createGeotagDialog(event.geotagDialog).show()
                        }
                    }
                }
            }

            vm.showErrorMessageEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
                SnackBarUtil.showErrorSnackBar(view, it)
            }

            vm.destination.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { consumable ->
                consumable.consume {
                    findNavController().navigate(it)
                }
            }

            setupTimeline()

            bAddGeotag.setOnClickListener {
                vm.onAddGeotagClick()
            }

            bSelectDate.setOnClickListener {
                vm.handleAction(SelectDateClickedAction)
            }

            lError.bReload.setOnClickListener {
                vm.handleAction(OnReloadPressedAction)
            }
        } catch (e: Exception) {
            vm.onError(e)
        }
    }

    override fun onResume() {
        super.onResume()
        vm.handleAction(OnResumeAction)
    }

    private fun setupTimeline() {
        bottomSheetBehavior = BottomSheetBehavior.from(lTimeline)
        bottomSheetBehavior.peekHeight = vm.style.summaryPeekHeight

        rvTimeline.adapter = timelineAdapter
        rvTimeline.layoutManager = LinearLayoutManager(MyApplication.context)

        scrim.setOnClickListener {
            vm.handleAction(OnScrimClickAction)
        }

        lTimelineHeader.setOnClickListener {
            vm.handleAction(OnTimelineHeaderClickAction)
        }

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
                vm.onBottomSheetStateChanged(newState)
            }
        })
    }

    private fun showDatePickerDialog(oldDate: LocalDate) {
        createDatePickerDialog(
            requireContext(),
            oldDate
        ) { newDate ->
            vm.handleAction(OnDateSelectedAction(newDate))
        }.show()
    }

    private fun displayLoadingState(isLoading: Boolean) {
        progress?.setGoneState(!isLoading)
        mapLoaderCanvas?.setGoneState(!isLoading)
        progress?.background = null
        if (isLoading) loader?.playAnimation() else loader?.cancelAnimation()
    }

    @SuppressLint("InflateParams")
    private fun createGeofenceVisitDialog(dialogData: GeofenceVisitDialog): AlertDialog {
        val containerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.inflate_timeline_dialog_gefence_visit, null, false)
        dialogData.visitId.toView(containerView.tvVisitId)
        dialogData.geofenceName.toView(containerView.tvTitle)
        dialogData.geofenceDescription.toViewOrHideIfNull(containerView.tvDescription)
        dialogData.integrationName.toViewOrHideIfNull(containerView.tvPlaceIntegrationName)
        dialogData.address.toViewOrHideIfNull(containerView.tvPlaceAddress)

        dialogData.routeToText?.toView(containerView.tvRouteTo)
        listOf(containerView.ivRouteTo, containerView.tvRouteTo).forEach {
            it.setGoneState(dialogData.routeToText == null)
        }

        containerView.bCopy.setOnClickListener {
            dialogData.visitId.let {
                vm.onCopyClick(it)
            }
        }


        val dialog = AlertDialog.Builder(requireContext())
            .setView(containerView)
            .create()

        containerView.bCloseDialog.setOnClickListener { dialog.dismiss() }

        val listener = { _: View ->
            dialog.dismiss()
            vm.handleAction(OnGeofenceClickAction(dialogData.visitId))
        }
        containerView.apply {
            listOf(
                tvTitle,
                tvDescription,
                tvPlaceIntegrationName,
                tvPlaceAddress
            ).forEach { it.setOnClickListener(listener) }
        }

        return dialog
    }

    private fun createGeotagDialog(dialogData: GeotagDialog): AlertDialog {
        val containerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.inflate_timeline_dialog_geotag, null, false)
        dialogData.geotagId.toView(containerView.tvGeotagId)
        dialogData.title.toView(containerView.tvTitle)
        dialogData.metadataString.toView(containerView.tvGeotagMetadata)
        dialogData.address.toViewOrHideIfNull(containerView.tvGeotagAddress)

        dialogData.routeToText?.toView(containerView.tvRouteTo)
        listOf(containerView.ivRouteTo, containerView.tvRouteTo).forEach {
            it.setGoneState(dialogData.routeToText == null)
        }

        containerView.bCopyId.setOnClickListener {
            dialogData.geotagId.let {
                vm.onCopyClick(it)
            }
        }

        containerView.bCopyMetadata.setOnClickListener {
            dialogData.metadataString.let {
                vm.onCopyClick(it)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(containerView)
            .create()

        containerView.bCloseDialog.setOnClickListener { dialog.dismiss() }
        return dialog
    }

    override fun onBackPressed(): Boolean {
        return if (super.onBackPressed()) {
            true
        } else {
            vm.onBackPressed()
        }
    }

    companion object {
        const val ANIMATION_DURATION = 200L
    }
}

