package com.hypertrack.android.ui.screens.sign_in

import com.hypertrack.android.utils.HardwareId

sealed class DeeplinkIssuesDialogState {
    override fun toString(): String = javaClass.simpleName
}
data class Displayed(val hardwareId: HardwareId) : DeeplinkIssuesDialogState()
object Hidden : DeeplinkIssuesDialogState()
