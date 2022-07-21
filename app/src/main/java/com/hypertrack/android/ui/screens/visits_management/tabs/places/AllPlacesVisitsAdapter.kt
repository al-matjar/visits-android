package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.ui.common.util.toViewOrHideIfNull
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter

import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_day.view.tvDayTitle
import kotlinx.android.synthetic.main.item_day.view.tvTotal
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.bCopy
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.ivRouteTo
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvDateTime
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvDescription
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvPlaceAddress
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvPlaceIntegrationName
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvRouteTo
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvTitle
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.tvVisitId


class AllPlacesVisitsAdapter(
    private val dateTimeFormatter: DateTimeFormatter,
    private val distanceFormatter: DistanceFormatter,
    private val onCopyClickListener: ((String) -> Unit)
) : BaseAdapter<VisitItem, BaseAdapter.BaseVh<VisitItem>>() {

    override val itemLayoutResource: Int = R.layout.item_place_visit_all_places

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Day -> Day::class.java.hashCode()
            is Visit -> Visit::class.java.hashCode()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseVh<VisitItem> {
        return createViewHolder(
            LayoutInflater.from(parent.context).inflate(
                when (viewType) {
                    Day::class.java.hashCode() -> R.layout.item_day
                    Visit::class.java.hashCode() -> itemLayoutResource
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
                        }
                    }
                    is Visit -> {
                        item.visitId.toView(containerView.tvVisitId)
                        item.title.toView(containerView.tvTitle)
                        item.durationText.toViewOrHideIfNull(containerView.tvDescription)
                        item.dateTimeText.toViewOrHideIfNull(containerView.tvDateTime)
                        item.routeToText.toViewOrHideIfNull(containerView.tvRouteTo)
                        listOf(containerView.ivRouteTo, containerView.tvRouteTo).forEach {
                            it.setGoneState(item.routeToText == null)
                        }
                        containerView.bCopy.setOnClickListener {
                            item.visitId.let {
                                onCopyClickListener.invoke(it)
                            }
                        }
                        item.integrationName.toViewOrHideIfNull(containerView.tvPlaceIntegrationName)
                        item.addressText.toView(containerView.tvPlaceAddress)
                    }
                }
            }
        }
    }
}



