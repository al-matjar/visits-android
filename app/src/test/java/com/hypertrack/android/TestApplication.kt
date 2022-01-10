package com.hypertrack.android

import android.app.Application
import com.hypertrack.android.utils.MyApplication
import org.robolectric.TestLifecycleApplication
import java.lang.reflect.Method

class TestApplication : Application(), TestLifecycleApplication {

    override fun beforeTest(method: Method?) {

    }

    override fun prepareTest(test: Any?) {

    }

    override fun afterTest(method: Method?) {

    }
}