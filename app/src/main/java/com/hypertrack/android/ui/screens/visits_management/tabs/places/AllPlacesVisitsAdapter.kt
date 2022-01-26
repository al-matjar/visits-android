package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.delegates.GeofenceVisitDisplayDelegate
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.ui.common.util.toViewOrHideIfNull
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter

import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_day.view.tvDayTitle
import kotlinx.android.synthetic.main.item_day.view.tvTotal
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.bCopy
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.ivRouteTo
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvDescription
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvPlaceAddress
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvPlaceIntegrationName
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvRouteTo
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvTitle
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvVisitId


class AllPlacesVisitsAdapter(
    private val osUtilsProvider: OsUtilsProvider,
    private val displayDelegate: GeofenceVisitDisplayDelegate,
    private val dateTimeFormatter: DateTimeFormatter,
    private val distanceFormatter: DistanceFormatter,
    private val onCopyClickListener: ((String) -> Unit)
) : BaseAdapter<VisitItem, BaseAdapter.BaseVh<VisitItem>>() {

    override val itemLayoutResource: Int = R.layout.item_place_visit_all_places

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Day -> Day::class.java.hashCode()
            is Visit -> Visit::class.java.hashCode()
//            is MonthItem -> MonthItem::class.java.hashCode()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseVh<VisitItem> {
        return createViewHolder(
            LayoutInflater.from(parent.context).inflate(
                when (viewType) {
                    Day::class.java.hashCode() -> R.layout.item_day
                    Visit::class.java.hashCode() -> itemLayoutResource
//                    MonthItem::class.java.hashCode() -> R.layout.item_month
                    else -> throw IllegalStateException("viewType ${viewType}")
                },
                parent,
                false
            )
        ) { position ->
            if (items[position] is Visit) {
                onItemClickListener?.invoke(items[position])
            }
        }
    }

    override fun createViewHolder(
        view: View,
        baseClickListener: (Int) -> Unit
    ): BaseAdapter.BaseVh<VisitItem> {
        return object : BaseContainerVh<VisitItem>(view, baseClickListener) {
            override fun bind(item: VisitItem) {
                when (item) {
                    is Day -> {
                        containerView.tvDayTitle.text = dateTimeFormatter.formatDate(item.date)
                        containerView.tvTotal.text = item.totalDriveDistance.meters.let {
                            distanceFormatter.formatDistance(it)
                        } ?: osUtilsProvider.stringFromResource(R.string.places_visits_loading)
                    }
                    is Visit -> {
                        val visit = item.visit
                        visit.id?.toView(containerView.tvVisitId)
                        displayDelegate.getGeofenceName(visit).toView(containerView.tvTitle)
                        displayDelegate.getDurationText(visit)
                            .toViewOrHideIfNull(containerView.tvDescription)
                        displayDelegate.getRouteToText(visit)
                            .toViewOrHideIfNull(containerView.tvRouteTo)
                        listOf(containerView.ivRouteTo, containerView.tvRouteTo).forEach {
                            it.setGoneState(visit.routeTo == null)
                        }
                        containerView.bCopy.setOnClickListener {
                            visit.id?.let {
                                onCopyClickListener.invoke(it)
                            }
                        }
                        containerView.tvPlaceIntegrationName.text =
                            displayDelegate.getGeofenceName(visit)
                        containerView.tvPlaceAddress.text = visit.address
                    }
//                    is MonthItem -> {
////                        Log.v(
////                            "hypertrack-verbose",
////                            "adapter $item ${visitsData.monthStats} ${visitsData.monthStats}"
////                        )
//                        val monthTotal = visitsData.monthStats[item.month]?.let {
//                            timeDistanceFormatter.formatDistance(it)
//                        }
//                        containerView.tvTitle.text =
//                            item.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
//                        containerView.tvTotal.text = monthTotal
//                            ?: osUtilsProvider.stringFromResource(R.string.places_visits_loading)
//                    }
                }
            }
        }
    }
}



