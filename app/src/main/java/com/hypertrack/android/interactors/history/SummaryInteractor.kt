package com.hypertrack.android.interactors.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.hypertrack.android.models.EMPTY_HISTORY
import com.hypertrack.android.models.History
import com.hypertrack.android.models.HistoryError
import com.hypertrack.android.repository.HistoryRepository
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.toConsumable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class SummaryInteractor(
    private val historyInteractor: HistoryInteractorImpl
) {

    val summary = Transformations.map(historyInteractor.todayHistory) {
        (it ?: EMPTY_HISTORY).summary
    }

    fun refresh() {
        historyInteractor.refreshTodayHistory()
    }

}

@Deprecated("use graphql implementation")
class HistoryInteractorImpl(
    private val historyRepository: HistoryRepository,
    private val globalScope: CoroutineScope
) {

    //todo update timer
    private var lastUpdate: ZonedDateTime? = null

    val todayHistory: LiveData<History?> = Transformations.map(historyRepository.history) {
        it[LocalDate.now()]
    }

    val history = historyRepository.history

    val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun refreshTodayHistory() {
        //todo test
        if (lastUpdate == null || ChronoUnit.MINUTES.between(
                ZonedDateTime.now(),
                lastUpdate
            ) > UPDATE_TIMEOUT_MINUTES
        ) {
            val updateTime = ZonedDateTime.now()
            globalScope.launch {
                //adds today history to cache
                val res = historyRepository.getHistory(LocalDate.now())
                when (res) {
                    is History -> {
                        lastUpdate = updateTime
                    }
                    is HistoryError -> {
                        errorFlow.emit(res.error.toConsumable())
                    }
                }
            }
        }
    }

    companion object {
        const val UPDATE_TIMEOUT_MINUTES = 1
    }

}
