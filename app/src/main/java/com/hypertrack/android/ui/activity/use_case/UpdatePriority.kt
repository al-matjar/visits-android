package com.hypertrack.android.ui.activity.use_case

import com.google.android.play.core.install.model.AppUpdateType

sealed class UpdatePriority(val value: Int)
object Immediate : UpdatePriority(AppUpdateType.IMMEDIATE)
object Flexible : UpdatePriority(AppUpdateType.FLEXIBLE)
