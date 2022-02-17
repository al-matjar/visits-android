package com.hypertrack.android.use_case.login

import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.sdk.HyperTrack

// result of publishable key check
// if there is a publishableKey saved - user is logged in
sealed class UserLoginStatus
data class LoggedIn(
    val hyperTrackSdk: HyperTrack,
    val publishableKey: PublishableKey
) : UserLoginStatus()

object NotLoggedIn : UserLoginStatus()
