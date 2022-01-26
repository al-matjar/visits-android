package com.hypertrack.android.ui.screens.place_details

import android.view.View
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.delegates.GeofenceVisitDisplayDelegate
import com.hypertrack.android.ui.common.util.goneIfNull
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.ui.common.util.toViewOrHideIfNull
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_place_visit.view.*

class PlaceVisitsAdapter(
    private val displayDelegate: GeofenceVisitDisplayDelegate,
    private val onCopyClickListener: ((String) -> Unit)
) : BaseAdapter<LocalGeofenceVisit, BaseAdapter.BaseVh<LocalGeofenceVisit>>() {

    override val itemLayoutResource: Int = R.layout.item_place_visit

    override fun createViewHolder(
        view: View,
        baseClickListener: (Int) -> Unit
    ): BaseVh<LocalGeofenceVisit> {
        return object : BaseContainerVh<LocalGeofenceVisit>(view, baseClickListener) {
            override fun bind(item: LocalGeofenceVisit) {
                item.id?.toView(containerView.tvVisitId)
                displayDelegate.getVisitTimeText(item).toView(containerView.tvTitle)
                displayDelegate.getDurationText(item).let {
                    it.toViewOrHideIfNull(containerView.tvDescription)
                }
                displayDelegate.getRouteToText(item)?.toView(containerView.tvRouteTo)
                listOf(containerView.ivRouteTo, containerView.tvRouteTo).forEach {
                    it.setGoneState(item.routeTo == null)
                }
                containerView.bCopy.setOnClickListener {
                    item.id?.let {
                        onCopyClickListener.invoke(it)
                    }
                }
                containerView.divider.setGoneState(adapterPosition == itemCount - 1)
            }
        }
    }
}
