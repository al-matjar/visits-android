package com.hypertrack.android.ui.screens.visits_management.tabs.history

import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.interactors.app.state.HistorySuccessState
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.LoadingState
import java.time.LocalDate

sealed class HistoryScreenState {
    val selectedDay: LocalDate
        get() {
            return when (this) {
                is Initial -> date
                is MapReadyState -> date
            }
        }

    override fun toString(): String = javaClass.simpleName
}

data class Initial(
    val date: LocalDate
) : HistoryScreenState()

data class MapReadyState(
    val date: LocalDate,
    val map: HypertrackMapWrapper,
    val historyState: LoadingState<HistorySuccessState, ErrorMessage>,
    val viewEventHandle: MutableLiveData<Consumable<ViewEvent>>
) : HistoryScreenState() {
    override fun toString(): String {
        return listOf(
            "date=$date",
            "historyState=$historyState",
        ).joinToString(", ").let {
            "${javaClass.simpleName}($it)"
        }
    }
}

