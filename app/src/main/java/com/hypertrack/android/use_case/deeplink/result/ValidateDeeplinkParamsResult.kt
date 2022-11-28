package com.hypertrack.android.use_case.deeplink.result

import com.hypertrack.android.interactors.app.UserAuthData

sealed class ValidateDeeplinkParamsResult
data class DeeplinkParamsValid(
    val userAuthData: UserAuthData,
    val deeplinkWithoutGetParams: String?
) : ValidateDeeplinkParamsResult()

data class DeeplinkParamsInvalid(val failure: WrongDeeplinkParams) : ValidateDeeplinkParamsResult()
