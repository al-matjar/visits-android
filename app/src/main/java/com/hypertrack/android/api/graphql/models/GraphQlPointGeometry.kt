package com.hypertrack.android.api.graphql.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class GraphQlPointGeometry(
    val center: List<Double>?,
    val vertices: List<List<Double>>?,
)
