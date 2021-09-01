package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.view.View
import com.hypertrack.android.delegates.GeofenceNameDelegate
import com.hypertrack.android.models.Location
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.delegates.GeofenceAddressDelegate
import com.hypertrack.android.ui.common.util.LocationUtils
import com.hypertrack.android.ui.common.util.formatDateTime
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.TimeDistanceFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_place.view.*


class PlacesAdapter(
    private val osUtilsProvider: OsUtilsProvider,
    private val locationProvider: DeviceLocationProvider,
    private val timeDistanceFormatter: TimeDistanceFormatter,
) : BaseAdapter<PlaceItem, BaseAdapter.BaseVh<PlaceItem>>() {

    private val addressDelegate = GeofenceAddressDelegate(osUtilsProvider)
    private val geofenceNameDelegate = GeofenceNameDelegate(osUtilsProvider)

    override val itemLayoutResource: Int = R.layout.item_place

    private var location: Location? = null

    init {
        locationProvider.getCurrentLocation {
            location = it
            notifyDataSetChanged()
        }
    }

    override fun createViewHolder(
        view: View,
        baseClickListener: (Int) -> Unit
    ): BaseAdapter.BaseVh<PlaceItem> {
        return object : BaseContainerVh<PlaceItem>(view, baseClickListener) {
            override fun bind(item: PlaceItem) {
                (item.geofence.visitsCount).let { visitsCount ->
                    listOf(containerView.tvLastVisit, containerView.ivLastVisit).forEach {
                        it.setGoneState(visitsCount == 0)
                    }
                    if (visitsCount > 0) {
                        val timesString =
                            MyApplication.context.resources.getQuantityString(
                                R.plurals.time,
                                visitsCount
                            )

                        "${
                            MyApplication.context.getString(
                                R.string.places_visited,
                                visitsCount.toString()
                            )
                        } $timesString"
                            .toView(containerView.tvVisited)

                        item.geofence.lastVisit?.arrival?.recordedAt?.formatDateTime()
                            ?.let {
                                MyApplication.context.getString(
                                    R.string.places_last_visit,
                                    it
                                )
                            }
                            ?.toView(containerView.tvLastVisit)

                    } else {
                        containerView.tvVisited.setText(R.string.places_not_visited)
                    }
                }

                val name = geofenceNameDelegate.getName(item.geofence)
                name.toView(containerView.tvTitle)

                val address = addressDelegate.shortAddress(item.geofence)
                address.toView(containerView.tvAddress)

                containerView.tvDistance.setGoneState(location == null)
                timeDistanceFormatter.formatDistance(
                    LocationUtils.distanceMeters(
                        location,
                        item.geofence.location
                    ) ?: -1
                ).toView(containerView.tvDistance)
            }
        }
    }
}

class PlaceItem(
    val geofence: LocalGeofence
)