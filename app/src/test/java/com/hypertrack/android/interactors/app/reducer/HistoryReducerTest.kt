package com.hypertrack.android.interactors.app.reducer

import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.interactors.app.HistoryAppAction
import com.hypertrack.android.interactors.app.LoadHistoryEffect
import com.hypertrack.android.interactors.app.action.StartDayHistoryLoadingAction
import com.hypertrack.android.interactors.app.state.HistoryState
import com.hypertrack.android.interactors.app.state.HistoryStateTest.Companion.historyState
import com.hypertrack.android.models.local.History
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.Loading
import com.hypertrack.android.utils.LoadingFailure
import com.hypertrack.android.utils.LoadingState
import com.hypertrack.android.utils.LoadingSuccess
import com.hypertrack.android.utils.state_machine.ReducerResult
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Suppress("ReplaceSizeZeroCheckWithIsEmpty")
class HistoryReducerTest {

    @Test
    fun `StartDayHistoryLoadingAction(today) - not loaded`() {
        val loadingDay = LocalDate.now()
        reduce(
            initialState = historyState(mapOf()),
            loadingDay = loadingDay
        ).let { result ->
            assertStateAndEffects(
                result,
                expectedState = mapOf(loadingDay to Loading()),
                loadingDay = loadingDay
            )
        }
    }

    @Test
    fun `StartDayHistoryLoadingAction(today) - loaded (timeout haven't ended)`() {
        val loadingDay = LocalDate.now()
        val initialState: Map<LocalDate, LoadingState<History, ErrorMessage>> =
            mapOf(loadingDay to LoadingSuccess(mockk()))
        val lastTodayReload = ZonedDateTime.now().minusNanos(
            // half of the time passed
            HistoryReducer.HISTORY_RELOAD_TIMEOUT * 1000L / 2
        )
        reduce(
            initialState = historyState(
                initialState,
                lastTodayReload = lastTodayReload
            ),
            loadingDay = loadingDay
        ).let { result ->
            assertStateAndEffects(
                result,
                expectedState = initialState,
                expectedLastTodayReload = lastTodayReload,
                loadingDay = null
            )
        }
    }

    @Test
    fun `StartDayHistoryLoadingAction(today) - loaded (timeout have ended)`() {
        val loadingDay = LocalDate.now()
        val initialState: Map<LocalDate, LoadingState<History, ErrorMessage>> =
            mapOf(loadingDay to LoadingSuccess(mockk()))
        reduce(
            initialState = historyState(
                initialState,
                lastTodayReload = ZonedDateTime.ofInstant(
                    Instant.EPOCH,
                    ZoneId.systemDefault()
                )
            ),
            loadingDay = loadingDay
        ).let { result ->
            assertStateAndEffects(
                result,
                expectedState = mapOf(loadingDay to Loading()),
                loadingDay = loadingDay
            )
        }
    }

    @Test
    fun `StartDayHistoryLoadingAction(today) - loading`() {
        val loadingDay = LocalDate.now()
        val initialState: Map<LocalDate, LoadingState<History, ErrorMessage>> =
            mapOf(loadingDay to Loading())
        reduce(
            initialState = historyState(initialState),
            loadingDay = loadingDay
        ).let { result ->
            assertStateAndEffects(
                result,
                expectedState = initialState,
                loadingDay = null
            )
        }
    }

    @Test
    fun `StartDayHistoryLoadingAction(today) - error`() {
        val loadingDay = LocalDate.now()
        val initialState: Map<LocalDate, LoadingState<History, ErrorMessage>> =
            mapOf(LocalDate.now() to LoadingFailure(ErrorMessage("")))
        reduce(
            initialState = historyState(initialState),
            loadingDay = loadingDay
        ).let { result ->
            assertStateAndEffects(
                result,
                expectedState = mapOf(loadingDay to Loading()),
                loadingDay = loadingDay
            )
        }
    }

    @Test
    fun `StartDayHistoryLoadingAction(not today) - not loaded`() {
        val loadingDay = LocalDate.now().minusDays(1)
        val initialState: Map<LocalDate, LoadingState<History, ErrorMessage>> =
            mapOf()
        reduce(
            initialState = historyState(initialState),
            loadingDay = loadingDay
        ).let { result ->
            assertStateAndEffects(
                result,
                expectedState = mapOf(loadingDay to Loading()),
                loadingDay = loadingDay
            )
        }
    }

    @Test
    fun `StartDayHistoryLoadingAction(not today) - loaded`() {
        val loadingDay = LocalDate.now().minusDays(1)
        val initialState: Map<LocalDate, LoadingState<History, ErrorMessage>> =
            mapOf(loadingDay to LoadingSuccess(mockk()))
        reduce(
            initialState = historyState(initialState),
            loadingDay = loadingDay
        ).let { result ->
            assertStateAndEffects(
                result,
                expectedState = initialState,
                loadingDay = null
            )
        }
    }

    @Test
    fun `StartDayHistoryLoadingAction(not today) - loading`() {
        val loadingDay = LocalDate.now().minusDays(1)
        val initialState: Map<LocalDate, LoadingState<History, ErrorMessage>> =
            mapOf(loadingDay to Loading())
        reduce(
            initialState = historyState(initialState),
            loadingDay = loadingDay
        ).let { result ->
            assertStateAndEffects(
                result,
                expectedState = initialState,
                loadingDay = null
            )
        }
    }

    @Test
    fun `StartDayHistoryLoadingAction(not today) - error`() {
        val loadingDay = LocalDate.now().minusDays(1)
        val initialState: Map<LocalDate, LoadingState<History, ErrorMessage>> =
            mapOf(loadingDay to LoadingFailure(ErrorMessage("")))
        reduce(
            initialState = historyState(initialState),
            loadingDay = loadingDay
        ).let { result ->
            assertStateAndEffects(
                result,
                expectedState = mapOf(loadingDay to Loading()),
                loadingDay = loadingDay
            )
        }
    }

    private fun reduce(
        initialState: HistoryState,
        loadingDay: LocalDate
    ): ReducerResult<out HistorySubState, out AppEffect> {
        return historyReducer().reduce(
            HistoryAppAction(StartDayHistoryLoadingAction(loadingDay)),
            userLoggedIn(),
            HistorySubState(
                history = initialState,
                historyScreenState = null
            )
        )
    }

    private fun assertStateAndEffects(
        result: ReducerResult<out HistorySubState, out AppEffect>,
        expectedState: Map<LocalDate, LoadingState<History, ErrorMessage>>,
        expectedLastTodayReload: ZonedDateTime? = null,
        loadingDay: LocalDate?
    ) {
        println(result)
        assertEquals(
            historyState(days = expectedState, lastTodayReload = expectedLastTodayReload),
            result.newState.history
        )
        if (loadingDay != null) {
            assertEquals(1, result.effects.size)
            result.effects.filterIsInstance<LoadHistoryEffect>().first().let {
                assertEquals(loadingDay, it.date)
            }
        } else {
            assertEquals(0, result.effects.size)
        }
    }

    companion object {
        fun historyReducer(): HistoryReducer {
            return HistoryReducer(
                appScope = mockk(),
                historyViewReducer = historyViewReducer()
            )
        }

        private fun historyViewReducer(): HistoryViewReducer {
            return HistoryViewReducer(
                appScope = mockk()
            )
        }
    }

}
