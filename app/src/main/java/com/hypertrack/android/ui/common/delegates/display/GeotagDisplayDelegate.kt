package com.hypertrack.android.ui.common.delegates.display

import com.hypertrack.android.models.local.Geotag
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.mapToJson
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi

class GeotagDisplayDelegate(
    private val osUtilsProvider: OsUtilsProvider,
    private val dateTimeFormatter: DateTimeFormatter,
    private val moshi: Moshi
) {

    fun getTimeTextForTimeline(geotag: Geotag): String {
        return dateTimeFormatter.formatTime(geotag.createdAt)
    }

    fun formatMetadata(geotag: Geotag): String {
        return moshi.mapToJson(geotag.metadata)
    }

    fun getDescription(geotag: Geotag): String {
        return osUtilsProvider.stringFromResource(
            R.string.geotag, dateTimeFormatter.formatDateTime(geotag.createdAt)
        )
    }

    fun getRouteToText(geotag: Geotag): String? {
        //todo implement getting geotag route to
        return null
    }

}
