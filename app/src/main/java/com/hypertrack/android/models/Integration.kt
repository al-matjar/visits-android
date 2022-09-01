package com.hypertrack.android.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class Integration(
    val id: String,
    val name: String?,
) : Parcelable {

    val type: String
        get() = "Company"

}
