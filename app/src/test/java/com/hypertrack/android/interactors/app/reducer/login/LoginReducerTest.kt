package com.hypertrack.android.interactors.app.reducer.login

import io.mockk.mockk
import junit.framework.TestCase

class LoginReducerTest {
    companion object {
        fun loginReducer() = LoginReducer(mockk())
    }
}
