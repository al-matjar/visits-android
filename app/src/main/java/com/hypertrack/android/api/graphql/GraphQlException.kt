package com.hypertrack.android.api.graphql

import com.hypertrack.android.api.graphql.queries.GraphQlQuery
import com.hypertrack.android.utils.exception.BaseException

class GraphQlException(
    query: GraphQlQuery,
    errors: List<GraphQlApi.GraphQlError>?
) : BaseException(
    mapOf(
        "query" to query.queryString,
        "errors" to errors?.joinToString(",\n\n")
    ).toString()
)
