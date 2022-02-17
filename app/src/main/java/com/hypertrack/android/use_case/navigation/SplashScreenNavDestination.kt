package com.hypertrack.android.use_case.navigation

sealed class SplashScreenNavDestination
object DoNothing : SplashScreenNavDestination()
object SignIn : SplashScreenNavDestination()
object VisitsManagement : SplashScreenNavDestination()
object RequestPermissions : SplashScreenNavDestination()
object RequestBackgroundLocation : SplashScreenNavDestination()
