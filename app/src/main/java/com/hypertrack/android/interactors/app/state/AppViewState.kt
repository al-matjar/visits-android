package com.hypertrack.android.interactors.app.state

sealed class AppViewState {
    override fun toString(): String = javaClass.simpleName

    fun isForScreen(screen: Screen): Boolean {
        return when (this) {
            AddGeotagScreenView -> screen is AddGeotagScreen
            AddIntegrationScreenView -> screen is AddIntegrationScreen
            AddOrderInfoScreenView -> screen is AddOrderInfoScreen
            AddPlaceInfoScreenView -> screen is AddPlaceInfoScreen
            AddPlaceScreenView -> screen is AddPlaceScreen
            BackgroundPermissionsScreenView -> screen is BackgroundPermissionsScreen
            ConfirmEmailScreenView -> screen is ConfirmEmailScreen
            NoneScreenView -> false
            OrderDetailsScreenView -> screen is OrderDetailsScreen
            OutageScreenView -> screen is OutageScreen
            PermissionsScreenView -> screen is PermissionsScreen
            PlaceDetailsScreenView -> screen is PlaceDetailsScreen
            SelectDestinationScreenView -> screen is SelectDestinationScreen
            SendFeedbackScreenView -> screen is SendFeedbackScreen
            SignInScreenView -> screen is SignInScreen
            SplashScreenView -> screen is SplashScreen
            is TabsView -> screen is TabsScreen
        }
    }
}

// not logged in screens
object NoneScreenView : AppViewState()
object SplashScreenView : AppViewState()
object SignInScreenView : AppViewState()
object ConfirmEmailScreenView : AppViewState()

// user scope screens
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
object OutageScreenView : AppViewState()
object SendFeedbackScreenView : AppViewState()
data class TabsView(
    val historyTab: HistoryTab?,
    val currentTripTab: CurrentTripTab = CurrentTripTab,
    val ordersTab: OrdersTab = OrdersTab,
    val placesTab: PlacesTab = PlacesTab,
    val summaryTab: SummaryTab = SummaryTab,
    val profileTab: ProfileTab = ProfileTab,
) : AppViewState()
