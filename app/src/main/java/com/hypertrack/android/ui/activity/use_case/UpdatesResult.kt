package com.hypertrack.android.ui.activity.use_case

import com.google.android.play.core.appupdate.AppUpdateInfo

sealed class UpdatesResult
object NoUpdates : UpdatesResult()
data class UpdateAvailable(
    val appUpdateInfo: AppUpdateInfo,
    val priority: UpdatePriority
) : UpdatesResult()
