package com.hypertrack.android.interactors

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hypertrack.android.api.MainCoroutineScopeRule
import com.hypertrack.android.repository.HistoryRepositoryImpl
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.mock.MockData
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import java.lang.Thread.sleep
import java.time.LocalDate

@Suppress("EXPERIMENTAL_API_USAGE")
class HistoryInteractorImplTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `it should load history days concurrently`() {
        val scope = CoroutineScope(Dispatchers.IO)
        val interactor = HistoryInteractorImpl(
            HistoryRepositoryImpl(
                mockk() {
                    coEvery { getHistory(any<LocalDate>(), any()) } returns MockData.EMPTY_HISTORY
                },
                mockk(relaxed = true),
                mockk(relaxed = true),
            ),
            scope
        )
        interactor.refreshTodayHistory()

        interactor.history.observeForever {
            println(it.size)
        }

        val dayCount = 1000

        runBlocking {
            for (i in 1..dayCount) {
                interactor.refreshTodayHistory()
                interactor.loadHistory(LocalDate.now().plusDays(i.toLong()))
            }
        }

        while (true) {
            sleep(500)
            //including today
            if (!scope.isActive || interactor.history.requireValue().size == dayCount + 1) {
                break
            }
        }
        assertEquals(dayCount + 1, interactor.history.requireValue().size)
    }

}
