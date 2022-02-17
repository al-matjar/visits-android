package com.hypertrack.android.use_case.deeplink

import com.hypertrack.android.interactors.app.UserAuthData

sealed class ValidateDeeplinkResult
data class DeeplinkValid(
    val userAuthData: UserAuthData,
    val deeplinkWithoutGetParams: String?
) : ValidateDeeplinkResult()

data class DeeplinkValidationError(val failure: DeeplinkFailure) : ValidateDeeplinkResult()
