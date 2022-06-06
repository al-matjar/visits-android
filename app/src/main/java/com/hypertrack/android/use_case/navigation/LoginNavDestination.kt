package com.hypertrack.android.use_case.navigation

sealed class LoginNavDestination
object VisitsManagement : LoginNavDestination()
object RequestPermissions : LoginNavDestination()
object RequestBackgroundLocation : LoginNavDestination()
