package com.hypertrack.android.ui.common.map.entities

import com.google.android.gms.maps.model.Circle

class MapCircle(
    circle: Circle
) : MapShape<Circle>(circle) {
    override fun remove() {
        shape.remove()
    }
}
