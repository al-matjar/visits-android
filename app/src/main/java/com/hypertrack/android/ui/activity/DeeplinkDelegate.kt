package com.hypertrack.android.ui.activity

import android.app.Activity
import android.content.Intent
import com.hypertrack.android.deeplink.BranchWrapper
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.deeplink.NoDeeplink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class DeeplinkDelegate(
    private val coroutineScope: CoroutineScope,
    private val branchWrapper: BranchWrapper
) {

    private val _deeplinkFlow = MutableSharedFlow<DeeplinkResult>(replay = 0)
    val deeplinkFlow: Flow<DeeplinkResult> = _deeplinkFlow

    fun onActivityStart(activity: Activity, intent: Intent?) {
        branchWrapper.activityOnStart(activity, intent?.data) {
            coroutineScope.launch { _deeplinkFlow.emit(it) }
        }
    }

    fun onActivityNewIntent(activity: Activity, intent: Intent) {
        branchWrapper.activityOnNewIntent(activity, intent) {
            coroutineScope.launch { _deeplinkFlow.emit(it) }
        }
    }

}
