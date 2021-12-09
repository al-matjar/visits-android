package com.hypertrack.android.ui.screens.visits_management.tabs.history

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.hypertrack.android.models.HistoryTileType
import com.hypertrack.android.models.Status
import com.hypertrack.logistics.android.github.R

interface HistoryStyle {
    val activeColor: Int
    val driveSelectionColor: Int
    val walkSelectionColor: Int
    val stopSelectionColor: Int
    val outageSelectionColor: Int
    val mapPadding: Int
    val summaryPeekHeight: Int
    fun colorForStatus(status: Status): Int
    fun markerForStatus(status: Status): Bitmap
}

class BaseHistoryStyle(private val context: Context) : HistoryStyle, TimelineStyle {
    override val activeColor: Int
        get() = context.resources.getColor(R.color.colorHistoryActiveSegment, context.theme)
    override val driveSelectionColor: Int
        get() = context.resources.getColor(R.color.colorHistorySelectedSegmentDrive, context.theme)
    override val walkSelectionColor: Int
        get() = context.resources.getColor(R.color.colorHistorySelectedSegmentWalk, context.theme)
    override val stopSelectionColor: Int
        get() = context.resources.getColor(R.color.colorHistorySelectedSegmentStop, context.theme)
    override val outageSelectionColor: Int
        get() = context.resources.getColor(R.color.colorHistoryOutageSegment, context.theme)
    override val mapPadding: Int
        get() = context.resources.getDimension(R.dimen.history_map_padding).toInt()
    override val summaryPeekHeight: Int by lazy {
        context.resources.getDimension(R.dimen.history_summary_peek_height).toInt()
    }

    override fun colorForStatus(status: Status): Int =
        when (status) {
            Status.STOP -> stopSelectionColor
            Status.DRIVE -> driveSelectionColor
            Status.WALK -> walkSelectionColor
            Status.OUTAGE -> outageSelectionColor
            else -> activeColor
        }

    override fun markerForStatus(status: Status): Bitmap =
        when (status) {
            Status.STOP -> asBitmap(R.drawable.ic_ht_bubble_activity_stop)
            Status.WALK -> asBitmap(R.drawable.ic_ht_bubble_activity_walk)
            Status.DRIVE -> asBitmap(R.drawable.ic_ht_bubble_activity_drive)
            Status.OUTAGE, Status.INACTIVE  -> asBitmap(R.drawable.ic_ht_activity_inactive)
            else -> asBitmap(R.drawable.ic_ht_status_marker_boundary)
        }

    private fun asBitmap(resource: Int):  Bitmap =
        ResourcesCompat.getDrawable(context.resources, resource, context.theme)!!.toBitmap()

    override fun summaryIcon() = R.drawable.ic_ht_eta

    override fun eventIcon(): Int = R.drawable.ic_ht_geofence_visited_active

    override fun iconForStatus(status: Status): Int =
        when (status) {
            Status.DRIVE -> R.drawable.ic_ht_drive
            Status.WALK -> R.drawable.ic_ht_walk
            Status.STOP -> R.drawable.ic_ht_stop
            Status.OUTAGE, Status.INACTIVE  -> R.drawable.ic_ht_activity_inactive
            Status.UNKNOWN -> R.drawable.ic_ht_status_marker_boundary
        }

    override fun statusImageForTile(type: HistoryTileType): Int =
        when(type)  {
            HistoryTileType.ACTIVE ->  R.drawable.ic_ht_timeline_active
            HistoryTileType.ACTIVE_START ->  R.drawable.ic_ht_timeline_active_start
            HistoryTileType.OUTAGE ->  R.drawable.ic_ht_timeline_outage
            HistoryTileType.OUTAGE_START ->  R.drawable.ic_ht_timeline_outage_start
            HistoryTileType.SUMMARY -> R.drawable.ic_ht_eta
    }

    override fun textColorForType(tileType: HistoryTileType) = ResourcesCompat.getColor(context.resources,
        when (tileType) {
        HistoryTileType.OUTAGE_START, HistoryTileType.OUTAGE ->  R.color.textOutage
        else -> R.color.textNormal
    }, context.theme)
}