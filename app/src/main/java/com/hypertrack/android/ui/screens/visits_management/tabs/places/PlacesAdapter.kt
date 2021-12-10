package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.view.View
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.delegates.GeofenceNameDelegate
import com.hypertrack.android.models.Location
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.delegates.GeofenceAddressDelegate
import com.hypertrack.android.ui.common.util.LocationUtils
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.utils.DeviceLocationProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.formatters.DatetimeFormatter

import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_place.view.*


class PlacesAdapter(
    private val osUtilsProvider: OsUtilsProvider,
    private val locationProvider: DeviceLocationProvider,
    private val distanceFormatter: DistanceFormatter,
    private val datetimeFormatter: DatetimeFormatter,
) : BaseAdapter<PlaceItem, BaseAdapter.BaseVh<PlaceItem>>() {

    private val addressDelegate = GeofenceAddressDelegate(osUtilsProvider)
    private val geofenceNameDelegate = GeofenceNameDelegate(osUtilsProvider, datetimeFormatter)

    override val itemLayoutResource: Int = R.layout.item_place

    private var location: LatLng? = null

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

                        item.geofence.lastVisit?.arrival?.let {
                            MyApplication.context.getString(
                                R.string.places_last_visit,
                                datetimeFormatter.formatDatetime(it)
                            )
                        }?.toView(containerView.tvLastVisit)

                    } else {
                        containerView.tvVisited.setText(R.string.places_not_visited)
                    }
                }

                val name = geofenceNameDelegate.getName(item.geofence)
                name.toView(containerView.tvTitle)

                val address = addressDelegate.shortAddress(item.geofence)
                address.toView(containerView.tvAddress)

                containerView.tvDistance.setGoneState(location == null)
                distanceFormatter.formatDistance(
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
