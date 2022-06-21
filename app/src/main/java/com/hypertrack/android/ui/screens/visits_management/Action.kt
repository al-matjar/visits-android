package com.hypertrack.android.ui.screens.visits_management

sealed class Action {
    override fun toString(): String = javaClass.simpleName
}

data class TrackingSwitchClickedAction(val isChecked: Boolean) : Action()
object OnViewCreatedAction : Action()
