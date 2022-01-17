package com.hypertrack.android.deeplink

import android.app.Activity

sealed class Action
data class DeeplinkPasted(val activity: Activity, val link: String) : Action()
data class IntentReceived(
    val activity: Activity,
    val reInit: Boolean
) : Action()
