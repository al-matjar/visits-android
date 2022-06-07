package com.hypertrack.android.ui.screens.background_permissions

import android.app.Activity

sealed class Action
data class OnAllowClick(val activity: Activity) : Action()
data class OnPermissionsResult(val activity: Activity) : Action()
