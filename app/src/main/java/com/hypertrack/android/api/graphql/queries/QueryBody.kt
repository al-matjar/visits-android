package com.hypertrack.android.api.graphql.queries

import com.squareup.moshi.JsonClass

interface GraphQlQuery {
    val queryString: String
    val variables: Map<String, Any>
}

@JsonClass(generateAdapter = true)
open class QueryBody<T : GraphQlQuery>(
    val query: String,
    val variables: Map<String, Any>,
) {
    constructor(query: T) : this(query.queryString, query.variables)
}
