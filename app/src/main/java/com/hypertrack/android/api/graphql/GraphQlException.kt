package com.hypertrack.android.api.graphql

import com.hypertrack.android.api.graphql.queries.GraphQlQuery

class GraphQlException(
    query: GraphQlQuery,
    errors: List<GraphQlApi.GraphQlError>?
) : Exception(
    mapOf(
        "query" to query.queryString,
        "errors" to errors?.joinToString(",\n\n")
    ).toString()
)
