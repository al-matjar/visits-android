package com.hypertrack.android.ui.common.map.entities

import com.google.android.gms.maps.model.CircleOptions

sealed class MapShapeOptions<T>(val options: T)

class MapCircleOptions(options: CircleOptions) : MapShapeOptions<CircleOptions>(options)
