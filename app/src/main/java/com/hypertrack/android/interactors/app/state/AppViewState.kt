package com.hypertrack.android.interactors.app.state

sealed class AppViewState {
    override fun toString(): String = javaClass.simpleName
}

object NoneScreenView : AppViewState()
object SplashScreenView : AppViewState()
object SignInScreenView : AppViewState()
object AddPlaceScreenView : AppViewState()
object PlaceDetailsScreenView : AppViewState()
object SelectDestinationScreenView : AppViewState()
object OrderDetailsScreenView : AppViewState()
object AddGeotagScreenView : AppViewState()
object AddIntegrationScreenView : AppViewState()
object AddOrderInfoScreenView : AppViewState()
object AddPlaceInfoScreenView : AppViewState()
object BackgroundPermissionsScreenView : AppViewState()
object PermissionsScreenView : AppViewState()
object ConfirmEmailScreenView : AppViewState()
object OutageScreenView : AppViewState()
object SendFeedbackScreenView : AppViewState()
data class TabsView(
    val currentTripTab: CurrentTripTab?,
    val historyTab: HistoryTab?,
    val ordersTab: OrdersTab?,
    val summaryTab: SummaryTab?,
    val placesTab: PlacesTab?,
    val profileTab: ProfileTab?
) : AppViewState()
