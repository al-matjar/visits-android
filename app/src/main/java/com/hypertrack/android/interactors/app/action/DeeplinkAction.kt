package com.hypertrack.android.interactors.app.action

import com.hypertrack.android.interactors.app.AppAction

// todo move all deeplink actions to this class
sealed class DeeplinkAction {
    override fun toString(): String = javaClass.simpleName
}

object DeeplinkCheckStartedAction : DeeplinkAction()
object DeeplinkCheckTimeoutAction : DeeplinkAction()
