package com.hypertrack.android.ui.screens.sign_in

import com.hypertrack.android.utils.HardwareId

data class ViewState(
    val isLoginButtonEnabled: Boolean,
    val showDeeplinkIssuesDialog: Boolean,
    val hardwareId: HardwareId?
)
