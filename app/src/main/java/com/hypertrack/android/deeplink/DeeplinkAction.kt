package com.hypertrack.android.deeplink

import android.app.Activity

sealed class DeeplinkAction
data class DeeplinkPasted(val activity: Activity, val link: String) : DeeplinkAction()
data class IntentReceived(
    val activity: Activity,
    val reInit: Boolean
) : DeeplinkAction()
