package com.hypertrack.android.ui.screens.visits_management.tabs.history

import androidx.annotation.IntDef
import java.lang.IllegalStateException

sealed class BottomSheetState {
    companion object {
        fun fromInt(state: Int): BottomSheetState {
            return when (state) {
                1 -> Dragging
                2 -> Settling
                3 -> Expanded
                4 -> Collapsed
                5 -> Hidden
                6 -> HalfExpanded
                else -> throw IllegalArgumentException(state.toString())
            }
        }
    }
}

object Dragging : BottomSheetState()
object Settling : BottomSheetState()
object Expanded : BottomSheetState()
object Collapsed : BottomSheetState()
object Hidden : BottomSheetState()
object HalfExpanded : BottomSheetState()

