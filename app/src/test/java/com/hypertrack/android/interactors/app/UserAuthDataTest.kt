package com.hypertrack.android.interactors.app

import com.hypertrack.android.TestInjector
import com.hypertrack.android.models.local.Email
import com.hypertrack.android.models.local.RealPublishableKey

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
