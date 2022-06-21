package com.hypertrack.android.interactors.app.state

sealed class Screen {
    override fun toString(): String = javaClass.simpleName
}
object SplashScreen : Screen()
object SignInScreen : Screen()
object TabsScreen : Screen()
object AddPlaceScreen : Screen()
object PlaceDetailsScreen : Screen()
object SelectDestinationScreen : Screen()
object OrderDetailsScreen : Screen()
object AddGeotagScreen : Screen()
object AddIntegrationScreen : Screen()
object AddOrderInfoScreen : Screen()
object AddPlaceInfoScreen : Screen()
object BackgroundPermissionsScreen : Screen()
object PermissionsScreen : Screen()
object ConfirmEmailScreen : Screen()
object OutageScreen : Screen()
object SendFeedbackScreen : Screen()

