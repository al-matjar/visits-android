package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.annotation.SuppressLint
import android.view.View
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.delegates.GeofenceNameDelegate
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.util.LocationUtils
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.utils.DeviceLocationProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.formatters.DateTimeFormatter

import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_place.view.*


@SuppressLint("NotifyDataSetChanged")
class PlacesAdapter(
    private val osUtilsProvider: OsUtilsProvider,
    private val locationProvider: DeviceLocationProvider,
    private val distanceFormatter: DistanceFormatter,
    private val dateTimeFormatter: DateTimeFormatter,
    private val resourceProvider: ResourceProvider,
) : BaseAdapter<PlaceItem, BaseAdapter.BaseVh<PlaceItem>>() {

    private val geofenceNameDelegate = GeofenceNameDelegate(osUtilsProvider, dateTimeFormatter)

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
                (item.visitsCount).let { visitsCount ->
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
                            resourceProvider.stringFromResource(
                                R.string.places_visited,
                                visitsCount.toString()
                            )
                        } $timesString"
                            .toView(containerView.tvVisited)

                        item.lastVisit?.arrival?.let {
                            MyApplication.context.getString(
                                R.string.places_last_visit,
                                dateTimeFormatter.formatDateTime(it.value)
                            )
                        }?.toView(containerView.tvLastVisit)

                    } else {
                        containerView.tvVisited.setText(R.string.places_not_visited)
                    }
                }

                item.geofenceDisplayName.toView(containerView.tvTitle)
                item.displayAddress.toView(containerView.tvAddress)

                containerView.tvDistance.setGoneState(location == null)
                distanceFormatter.formatDistance(
                    LocationUtils.distanceMeters(
                        location,
                        item.location
                    ) ?: -1
                ).toView(containerView.tvDistance)
            }
        }
    }
}

class PlaceItem(
    geofence: Geofence,
    val displayAddress: String,
    val geofenceDisplayName: String
) {
    val geofenceId = geofence.id
    val visitsCount = geofence.visitsCount
    val lastVisit = geofence.lastVisit
    val location = geofence.location
}
