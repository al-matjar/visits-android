package com.hypertrack.android.ui.screens.permission_request

import android.app.Activity

sealed class Action
data class RequestPermissionsAction(val activity: Activity) : Action()
data class OnResumeAction(val activity: Activity) : Action()
object OnSkipClickedAction : Action()
