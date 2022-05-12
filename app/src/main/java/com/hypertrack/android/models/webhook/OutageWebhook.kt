package com.hypertrack.android.models.webhook

import com.hypertrack.android.api.Arrival
import com.hypertrack.android.models.GeofenceMetadata
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OutageWebhook(
    // entry, exit
    @field:Json(name = "inactive_reason") val inactiveReason: InactiveReason,
)

@JsonClass(generateAdapter = true)
data class InactiveReason(
    @field:Json(name = "type") val outageType: String,
    @field:Json(name = "name") val outageDisplayName: String,
    @field:Json(name = "code") val outageCode: String,
    @field:Json(name = "description") val outageDeveloperDescription: String,
    @field:Json(name = "user_action_required") val userActionRequired: String,
)
