package com.hypertrack.android.ui.common.select_destination.reducer

import com.hypertrack.android.ui.common.select_destination.GooglePlaceModel
import com.hypertrack.android.utils.NonEmptyList

sealed class UserFlow
data class AutocompleteFlow(
    val places: NonEmptyList<GooglePlaceModel>
) : UserFlow()

object MapFlow : UserFlow() {
    override fun toString(): String = javaClass.simpleName
}
