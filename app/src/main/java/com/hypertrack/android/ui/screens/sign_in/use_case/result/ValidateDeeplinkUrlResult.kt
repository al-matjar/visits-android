package com.hypertrack.android.ui.screens.sign_in.use_case.result

import android.net.Uri

sealed class ValidateDeeplinkUrlResult
data class ValidDeeplink(val url: String) : ValidateDeeplinkUrlResult()
data class BranchReferrer(val url: String) : ValidateDeeplinkUrlResult()
data class InvalidDeeplink(val url: String) : ValidateDeeplinkUrlResult()
