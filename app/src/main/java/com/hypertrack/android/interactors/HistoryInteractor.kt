package com.hypertrack.android.interactors

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.hypertrack.android.models.*
import com.hypertrack.android.repository.HistoryRepository
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.common.util.toHotTransformation
import com.hypertrack.android.utils.OsUtilsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

interface HistoryInteractor {
    val todayHistory: LiveData<History>
//    val monthSummaries: LiveData<Map<Month, Summary>>
    val history: LiveData<Map<LocalDate, History>>
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    fun loadTodayHistory()
    fun loadHistory(date: LocalDate)
//    fun loadMonthSummary(month: Month)
}

class HistoryInteractorImpl(
    private val historyRepository: HistoryRepository,
    private val osUtilsProvider: OsUtilsProvider,
    private val globalScope: CoroutineScope
) : HistoryInteractor {

    override val todayHistory = MutableLiveData<History>()

    override val history = Transformations.map(historyRepository.history) {
        HashMap(it).apply {
            todayHistory.value?.let { put(LocalDate.now(), it) }
        }.toMap()
    }.toHotTransformation().liveData

//    override val monthSummaries = MutableLiveData<Map<Month, Summary>>(mapOf())
//    private val _monthSummaries = mutableMapOf<Month, Summary>()

    override val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun loadTodayHistory() {
        globalScope.launch {
            val res = historyRepository.getHistory(LocalDate.now())
            when (res) {
                is History -> {
                    todayHistory.postValue(res)
                }
                is HistoryError -> {
                    errorFlow.emit((res.error ?: Exception("History error null")).toConsumable())
                }
            }
        }
    }

    override fun loadHistory(date: LocalDate) {
        globalScope.launch {
            historyRepository.getHistory(date)
        }
    }

//    override fun loadMonthSummary(month: Month) {
//        Log.v("hypertrack-verbose", "loadMonthSummary $month")
//        globalScope.launch {
//            try {
//                val firstDay = LocalDate.of(
//                    LocalDate.now().year,
//                    month,
//                    1
//                )
//                val from = ZonedDateTime.of(
//                    firstDay,
//                    LocalTime.MIN,
//                    osUtilsProvider.getTimeZoneId(),
//                )
//                val to = ZonedDateTime.of(
//                    firstDay.withDayOfMonth(firstDay.lengthOfMonth()),
//                    LocalTime.MIN,
//                    osUtilsProvider.getTimeZoneId(),
//                )
//                val res = historyRepository.getSummary(from, to)
//                _monthSummaries[month] = res
//                monthSummaries.postValue(_monthSummaries)
//            } catch (e: Exception) {
//                errorFlow.emit(e.toConsumable())
//            }
//        }
//    }

}