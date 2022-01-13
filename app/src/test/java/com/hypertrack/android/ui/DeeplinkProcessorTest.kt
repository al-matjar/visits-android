package com.hypertrack.android.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.hypertrack.android.utils.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DeeplinkProcessorTest {

    //team account, pavel@hypertrack.io
    private val deeplink = "https://hypertrack-logistics.app.link/1oF0VcDvYgb"
    private val uri: Uri = mockk()

    private suspend fun testOnStart(
        link: String?,
        branch: BranchWrapper = mockk(),
        asserts: (DeeplinkResult) -> Unit
    ) {
        BranchIoDeepLinkProcessor(mockk(relaxed = true), mockk(relaxed = true), branch).let {
            val activity: Activity = mockk() {
                every { intent } returns mockk() {
                    every { data } returns link?.let { uri }
                }
            }
            it.activityOnStart(activity).let {
                asserts.invoke(it)
            }
        }
    }

    private suspend fun testOnNew(
        link: String?,
        intentMockConfig: (Intent) -> Unit = {},
        branch: BranchWrapper = mockk(),
        asserts: (DeeplinkResult) -> Unit
    ) {
        BranchIoDeepLinkProcessor(mockk(relaxed = true), mockk(relaxed = true), branch).let {
            val activity: Activity = mockk() {
                every { intent } returns mockk() {
                    every { data } returns link?.let { uri }
                    intentMockConfig.invoke(this)
                }
            }
            it.activityOnNewIntent(activity).let {
                asserts.invoke(it)
            }
        }
    }

    @Test
    fun `it should correctly handle login without deeplink`() {
        runBlocking {
            testOnStart(null) {
                assertEquals(NoDeeplink, it)
            }

            testOnNew(null) {
                assertEquals(NoDeeplink, it)
            }
        }
    }

    @Test
    fun `it should correctly handle login with deeplink`() {
        val branch: BranchWrapper = mockk() {
            every { initSession(any(), any(), any(), any()) } answers {
                arg<(BranchResult) -> Unit>(3).invoke(BranchSuccess(hashMapOf("param1" to "value1")))
            }
        }

        runBlocking {
            testOnStart(deeplink, branch = branch) {
                println(it)
                assertEquals(mapOf("param1" to "value1"), (it as DeeplinkParams).parameters)
            }

            run {
                val slot = slot<Boolean>()
                testOnNew(
                    deeplink, branch = branch,
                    intentMockConfig = {
                        every { it.putExtra("branch_force_new_session", capture(slot)) } returns it
                    },
                ) {
                    println(it)
                    assertEquals(mapOf("param1" to "value1"), (it as DeeplinkParams).parameters)
                    assertEquals(true, slot.captured)
                }
            }

            BranchIoDeepLinkProcessor(mockk(relaxed = true), mockk(relaxed = true), branch).let {
                val slot = slot<Boolean>()
                val mockIntent: Intent = mockk() {
                    every { data } returns null
                    every { putExtra("branch_force_new_session", capture(slot)) } returns this
                }
                val activity: Activity = mockk() {
                    every { intent } returns mockIntent
                }
                it.onLinkRetrieved(activity, deeplink).let { result ->
                    println(result)
                    assertEquals(
                        mapOf("param1" to "value1"),
                        (result as DeeplinkParams).parameters
                    )
                    assertEquals(true, slot.captured)
                }
            }
        }
    }

    @Test
    fun `it should correctly handle branch errors`() {
        val exception = mockk<BranchErrorException>()
        val branch: BranchWrapper = mockk() {
            every { initSession(any(), any(), any(), any()) } answers {
                arg<(BranchResult) -> Unit>(3).invoke(
                    BranchError(exception)
                )
            }
        }

        runBlocking {
            testOnStart(deeplink, branch = branch) {
                println(it)
                assertEquals(exception, (it as DeeplinkError).exception)
            }

            testOnNew(
                deeplink, branch = branch,
                intentMockConfig = {
                    every { it.putExtra("branch_force_new_session", any<Boolean>()) } returns it
                }
            ) {
                println(it)
                assertEquals(exception, (it as DeeplinkError).exception)
            }

            BranchIoDeepLinkProcessor(mockk(relaxed = true), mockk(relaxed = true), branch).let {
                val mockIntent: Intent = mockk() {
                    every { data } returns null
                    every { putExtra("branch_force_new_session", any<Boolean>()) } returns this
                }
                val activity: Activity = mockk() {
                    every { intent } returns mockIntent
                }
                it.onLinkRetrieved(activity, deeplink).let { result ->
                    println(result)
                    assertEquals(exception, (result as DeeplinkError).exception)
                }
            }
        }
    }

    @Test
    fun `it should correctly match deeplink pattern`() {
        listOf(
            "https://hypertrack-logistics.app.link/1oF0VcDvYgb",
            "https://hypertrack-logistics.app.link/1oF0VcDvYgb?",
            "https://hypertrack-logistics.app.link/1oF0VcDvYgb?ddddddd",
            "https://hypertrack-logistics.app.link/1oF",
            "https://hypertrack-logistics.app.link/1oF0VcDvYgddddddddddddddd",
        ).forEach {
            DeeplinkProcessor.DEEPLINK_REGEX.matcher(it).matches().let {
                println(it)
                assertTrue(it)
            }
        }

        listOf(
            "https://hypertrack-logistics.app.link/",
            "https://google.com/1oF0VcDvYgddddddddddddddd"
        ).forEach {
            DeeplinkProcessor.DEEPLINK_REGEX.matcher(it).matches().let {
                println(it)
                assertFalse(it)
            }
        }
    }

}