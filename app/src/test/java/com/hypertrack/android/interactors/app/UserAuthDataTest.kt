package com.hypertrack.android.interactors.app

import com.hypertrack.android.TestInjector
import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.android.models.local.RealPublishableKey
import junit.framework.TestCase

class UserAuthDataTest {
    companion object {
        fun emailAuthData(
            email: Email = TestInjector.TEST_EMAIL,
            publishableKey: RealPublishableKey = TestInjector.TEST_PUBLISHABLE_KEY,
            metadata: Map<String, Any> = mapOf()
        ): EmailAuthData {
            return EmailAuthData(
                email,
                publishableKey,
                metadata
            )
        }
    }
}
