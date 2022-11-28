package com.hypertrack.android.use_case.deeplink.exception

import com.hypertrack.android.use_case.deeplink.result.WrongDeeplinkParams

class InvalidDeeplinkParamsException(val wrongDeeplinkParams: WrongDeeplinkParams) : Exception(
    wrongDeeplinkParams.toString()
)
