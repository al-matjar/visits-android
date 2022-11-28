package com.hypertrack.android.utils

import androidx.test.platform.app.InstrumentationRegistry
import com.hypertrack.android.interactors.PhotoForUpload
import com.hypertrack.android.interactors.PhotoUploadingState
import com.hypertrack.android.repository.MyPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test


@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class MyPreferencesTest {

    private lateinit var myPreferences: MyPreferences

    @Before
    fun setUp() {
        myPreferences =
            MyPreferences(
                InstrumentationRegistry.getInstrumentation().targetContext,
                TestInjector.getMoshi(),
                object : CrashReportsProvider {
                    override fun logException(exception: Throwable, metadata: Map<String, String>) {
                    }

                    override fun log(txt: String) {
                    }

                    override fun setUserIdentifier(id: String) {
                    }

                    override fun setCustomKey(key: String, value: String) {
                    }
                }
            )
        myPreferences.clearPreferences()
    }

    @Test
    fun crudPhotoUploadQueue() {
        runBlocking {
            myPreferences.addToPhotosQueue(
                PhotoForUpload(
                    "1",
                    "path",
                    "thumb",
                    PhotoUploadingState.ERROR
                ),
            )
            myPreferences.addToPhotosQueue(
                PhotoForUpload(
                    "2",
                    "path",
                    "thumb",
                    PhotoUploadingState.ERROR
                ),
            )
            myPreferences.getPhotosQueue().toList().let {
                assertEquals(2, it.size)
                assertEquals("1", it[0].photoId)
                assertEquals(PhotoUploadingState.ERROR, it[0].state)
            }
            myPreferences.updatePhotoState("2", PhotoUploadingState.UPLOADED)
            myPreferences.updatePhotoState("1", PhotoUploadingState.NOT_UPLOADED)
            myPreferences.getPhotosQueue().toList().let {
                assertEquals(1, it.size)
                assertEquals(PhotoUploadingState.NOT_UPLOADED, it[0].state)
            }
        }
    }

}
