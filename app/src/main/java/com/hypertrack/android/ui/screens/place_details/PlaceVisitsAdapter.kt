package com.hypertrack.android.ui.screens.place_details

import android.util.Log
import android.view.View
import com.hypertrack.android.api.GeofenceMarker
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_place_visit.view.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PlaceVisitsAdapter(
    private val osUtilsProvider: OsUtilsProvider,
    private val timeDistanceFormatter: TimeDistanceFormatter,
    private val onCopyClickListener: ((String) -> Unit)
) : BaseAdapter<GeofenceMarker, BaseAdapter.BaseVh<GeofenceMarker>>() {

    override val itemLayoutResource: Int = R.layout.item_place_visit

    override fun createViewHolder(
        view: View,
        baseClickListener: (Int) -> Unit
    ): BaseVh<GeofenceMarker> {
        return object : BaseContainerVh<GeofenceMarker>(view, baseClickListener) {
            override fun bind(item: GeofenceMarker) {
                bindVisit(
                    containerView,
                    item,
                    timeDistanceFormatter,
                    osUtilsProvider,
                    onCopyClickListener
                )

                containerView.divider.setGoneState(adapterPosition == itemCount - 1)
            }
        }
    }

    companion object {
        fun bindVisit(
            containerView: View,
            item: GeofenceMarker,
            timeDistanceFormatter: TimeDistanceFormatter,
            osUtilsProvider: OsUtilsProvider,
            onCopyClickListener: ((String) -> Unit)?
        ) {
            formatDate(
                item.arrival!!.recordedAt,
                item.exit?.recordedAt,
                osUtilsProvider,
                timeDistanceFormatter
            ).toView(containerView.tvTitle)
            item.duration?.let { DateTimeUtils.secondsToLocalizedString(it) }
                ?.toView(containerView.tvDescription)
            item.markerId?.toView(containerView.tvVisitId)
            item.routeTo?.let {
                if (it.distance == null) return@let null
                if (it.duration == null) return@let null
                MyApplication.context.getString(
                    R.string.place_route_ro,
                    timeDistanceFormatter.formatDistance(it.distance),
                    DateTimeUtils.secondsToLocalizedString(it.duration)
                )
            }?.toView(containerView.tvRouteTo)
            listOf(containerView.ivRouteTo, containerView.tvRouteTo).forEach {
                it.setGoneState(item.routeTo == null)
            }

            containerView.setOnClickListener {
                item.markerId?.let {
                    onCopyClickListener?.invoke(it)
                }
            }
        }

        //todo test
        fun formatDate(
            enter: String?,
            exit: String?,
            osUtilsProvider: OsUtilsProvider,
            timeDistanceFormatter: TimeDistanceFormatter
        ): String {
            val enterDt = ZonedDateTime.parse(enter)
            val exitDt = exit?.let { ZonedDateTime.parse(it) }
            val equalDay = enterDt.dayOfMonth == exitDt?.dayOfMonth


            //todo now
            return if (equalDay) {
                "${getDateString(enterDt, osUtilsProvider)}, ${
                    getTimeString(
                        enterDt,
                        osUtilsProvider,
                        timeDistanceFormatter
                    )
                } — ${getTimeString(exitDt, osUtilsProvider, timeDistanceFormatter)}"
            } else {
                "${getDateString(enterDt, osUtilsProvider)}, ${
                    getTimeString(
                        enterDt,
                        osUtilsProvider,
                        timeDistanceFormatter
                    )
                } — ${
                    exitDt?.let {
                        "${getDateString(exitDt, osUtilsProvider)}, ${
                            getTimeString(
                                exitDt,
                                osUtilsProvider,
                                timeDistanceFormatter
                            )
                        }"
                    } ?: osUtilsProvider.getString(R.string.now)
                }"
            }
        }

        private fun getDateString(it: ZonedDateTime, osUtilsProvider: OsUtilsProvider): String {
            val now = ZonedDateTime.now()
            val yesterday = ZonedDateTime.now().minusDays(1)
            return when {
                isSameDay(it, now) -> {
                    osUtilsProvider.stringFromResource(R.string.place_today)
                }
                isSameDay(it, yesterday) -> {
                    osUtilsProvider.stringFromResource(R.string.place_yesterday)
                }
                else -> {
                    it.formatDate()
                }
            }
        }

        private fun getTimeString(
            it: ZonedDateTime?,
            osUtilsProvider: OsUtilsProvider,
            timeDistanceFormatter: TimeDistanceFormatter
        ): String {
            return it?.let {
                timeDistanceFormatter.formatTime(it.format(DateTimeFormatter.ISO_INSTANT))
            } ?: osUtilsProvider.getString(R.string.now)
        }

        private fun isSameDay(date1: ZonedDateTime, date2: ZonedDateTime): Boolean {
            val d1 = date1.withZoneSameInstant(ZoneId.of("UTC"))
            val d2 = date2.withZoneSameInstant(ZoneId.of("UTC"))
            return d1.dayOfMonth == d2.dayOfMonth && d1.year == d2.year
        }
    }
}