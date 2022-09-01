package com.hypertrack.android.ui.screens.add_integration

import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.common.DataPage
import com.hypertrack.android.utils.Result


sealed class Action {
    override fun toString(): String = javaClass.simpleName
}

object InitAction : Action()
object OnLoadMoreAction : Action()
object OnRefreshAction : Action()
object OnInitSearchAction : Action()
data class OnQueryChangedAction(val query: String) : Action()
data class OnIntegrationsResultAction(val result: Result<DataPage<Integration>>) : Action()
data class OnIntegrationClickedAction(val integration: Integration) : Action()
