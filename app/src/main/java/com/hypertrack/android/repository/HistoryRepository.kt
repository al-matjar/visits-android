package com.hypertrack.android.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.models.History
import com.hypertrack.android.models.HistoryError
import com.hypertrack.android.models.HistoryResult
import com.hypertrack.android.models.Summary
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZonedDateTime

interface HistoryRepository {
    suspend fun getHistory(date: LocalDate): HistoryResult
//    suspend fun getSummary(from: ZonedDateTime, to: ZonedDateTime): Summary
    val history: LiveData<Map<LocalDate, History>>
}

class HistoryRepositoryImpl(
    private val apiClient: ApiClient,
    private val crashReportsProvider: CrashReportsProvider,
    private val osUtilsProvider: OsUtilsProvider
) : HistoryRepository {
    override val history = MutableLiveData<Map<LocalDate, History>>(mapOf())

    private val cache = mutableMapOf<LocalDate, History>()
    private val mutex = Mutex()
//    private val periodCache = mutableMapOf<String, History>()

    override suspend fun getHistory(date: LocalDate): HistoryResult {
        if (cache.containsKey(date) && date != LocalDate.now()) {
            return cache.getValue(date)
        } else {
            val res = apiClient.getHistory(
                date,
                osUtilsProvider.getTimeZoneId()
            )
            when (res) {
                is History -> {
                    mutex.withLock {
                        addDayToCache(date, res)
                    }
                }
                is HistoryError -> {
                    crashReportsProvider.logException(res.error)
                }
            }
            return res
        }
    }

//    override suspend fun getSummary(from: ZonedDateTime, to: ZonedDateTime): Summary {
//        val cacheKey = "$from-$to"
//        if (periodCache.containsKey(cacheKey) &&
//            to.isBefore(
//                ZonedDateTime.now()
//                    .withDayOfMonth(1)
//                    .withHour(0)
//                    .withMinute(0)
//            )
//        ) {
//            Log.v("hypertrack-verbose", "got cached period $cacheKey")
//            return periodCache.getValue(cacheKey).summary
//        } else {
//            apiClient.getHistory(from, to).let {
//                when (it) {
//                    is History -> {
//                        periodCache[cacheKey] = it
//                        Log.v("hypertrack-verbose", "got period $cacheKey")
//                        return it.summary
//                    }
//                    is HistoryError -> throw it.error!!
//                }
//            }
//        }
//    }

    private fun addDayToCache(date: LocalDate, res: History) {
        cache[date] = res
        history.postValue(cache)
    }

}
