package com.hypertrack.android.ui.screens.sign_in.use_case.result

import com.hypertrack.android.interactors.app.action.InitiateLoginAction
import com.hypertrack.android.use_case.deeplink.result.WrongDeeplinkParams

sealed class PastedDeeplinkResult
data class PasteSuccess(val action: InitiateLoginAction) : PastedDeeplinkResult()
data class InvalidParams(val error: WrongDeeplinkParams) : PastedDeeplinkResult()
object InvalidUrl : PastedDeeplinkResult()
