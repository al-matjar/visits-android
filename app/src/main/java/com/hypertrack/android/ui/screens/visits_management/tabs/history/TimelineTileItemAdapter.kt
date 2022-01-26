package com.hypertrack.android.ui.screens.visits_management.tabs.history

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.hypertrack.android.models.local.Drive
import com.hypertrack.android.models.local.Stop
import com.hypertrack.android.models.local.UnknownActivity
import com.hypertrack.android.models.local.Walk
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.util.goneIfNull
import com.hypertrack.android.ui.common.util.hide
import com.hypertrack.android.ui.common.util.setImageResourceNullable
import com.hypertrack.android.ui.common.util.show
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R

class TimelineTileItemAdapter(
    private val osUtilsProvider: OsUtilsProvider,
    private val onClickListener: (TimelineTile) -> Unit,
) : BaseAdapter<TimelineTile, TimelineTileItemAdapter.VhTimeLineTile>() {
    private val style = TimelineStyle(osUtilsProvider)

    override val itemLayoutResource = R.layout.item_history_tile

    override fun createViewHolder(view: View, baseClickListener: (Int) -> Unit): VhTimeLineTile {
        return VhTimeLineTile(view) {
            onClickListener.invoke(items[it])
        }
    }

    inner class VhTimeLineTile(holder: View, clickListener: (Int) -> Unit) :
        BaseAdapter.BaseVh<TimelineTile>(holder, clickListener) {
        private val activityIcon: AppCompatImageView = holder.findViewById(R.id.ivActivityIcon)
        private val activitySummary: AppCompatTextView = holder.findViewById(R.id.tvActivitySummary)
        private val activityPlace: AppCompatTextView = holder.findViewById(R.id.tvActivityPlace)
        private val activityTimeFrame: AppCompatTextView = holder.findViewById(R.id.tvTimeframe)
        private val statusStripe: AppCompatImageView = holder.findViewById(R.id.ivStatusStripe)
        private val eventIcon: AppCompatImageView = holder.findViewById(R.id.ivEventIcon)
        private val notch: View = holder.findViewById(R.id.notch)

        override fun bind(item: TimelineTile) {
            activityIcon.setImageResourceNullable(
                when (item.payload) {
                    is GeofenceVisitTile -> style.iconGeotag
                    is GeotagTile -> style.iconGeotag
                    is ActiveStatusTile -> when (item.payload.activity) {
                        Drive -> style.iconDrive
                        Stop -> style.iconStop
                        Walk -> style.iconWalk
                        UnknownActivity -> null
                    }
                    is InactiveStatusTile -> style.iconOutage
                }
            )
            activityTimeFrame.text = item.timeString
            activitySummary.text = item.description

            activitySummary.setTextColor(
                when (item.payload) {
                    is GeofenceVisitTile, is GeotagTile, is ActiveStatusTile -> style.textColor
                    is InactiveStatusTile -> {
                        style.textColorOutage
                    }
                }
            )

            activityPlace.text = item.address
            activityPlace.goneIfNull(item.address)

            statusStripe.setImageResource(
                if (item.isOutage) {
                    if (item.isStart) {
                        R.drawable.ic_ht_timeline_outage_start
                    } else {
                        R.drawable.ic_ht_timeline_outage
                    }
                } else {
                    if (item.isStart) {
                        R.drawable.ic_ht_timeline_active_start
                    } else {
                        R.drawable.ic_ht_timeline_active
                    }
                }
            )

            when (item.payload) {
                is ActiveStatusTile, is InactiveStatusTile -> {
                    eventIcon.hide()
                    eventIcon.setImageResource(style.lineIconEvent)
                }
                is GeofenceVisitTile, is GeotagTile -> {
                    eventIcon.show()
                }
            } as Any?
        }
    }

}

class TimelineStyle(osUtilsProvider: OsUtilsProvider) {
    val textColor = osUtilsProvider.colorFromResource(R.color.textNormal)
    val textColorOutage = osUtilsProvider.colorFromResource(R.color.textOutage)
    val iconOutage = R.drawable.ic_ht_activity_inactive
    val iconDrive = R.drawable.ic_ht_drive
    val iconStop = R.drawable.ic_ht_stop
    val iconWalk = R.drawable.ic_ht_walk
    val iconGeotag = R.drawable.ic_ht_status_marker_boundary

    //todo visit icon
    val iconVisit = R.drawable.ic_ht_status_marker_boundary
    val lineIconEvent = R.drawable.ic_ht_geofence_visited_active
}


