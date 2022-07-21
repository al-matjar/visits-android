package com.hypertrack.android.interactors.app.reducer

import com.hypertrack.android.interactors.app.AppActionEffect
import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.HistoryAppAction
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.action.StartDayHistoryLoadingAction
import com.hypertrack.android.interactors.app.state.AddGeotagScreen
import com.hypertrack.android.interactors.app.state.AddGeotagScreenView
import com.hypertrack.android.interactors.app.state.AddIntegrationScreen
import com.hypertrack.android.interactors.app.state.AddIntegrationScreenView
import com.hypertrack.android.interactors.app.state.AddOrderInfoScreen
import com.hypertrack.android.interactors.app.state.AddOrderInfoScreenView
import com.hypertrack.android.interactors.app.state.AddPlaceInfoScreen
import com.hypertrack.android.interactors.app.state.AddPlaceInfoScreenView
import com.hypertrack.android.interactors.app.state.AddPlaceScreen
import com.hypertrack.android.interactors.app.state.AddPlaceScreenView
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.AppViewState
import com.hypertrack.android.interactors.app.state.BackgroundPermissionsScreen
import com.hypertrack.android.interactors.app.state.BackgroundPermissionsScreenView
import com.hypertrack.android.interactors.app.state.ConfirmEmailScreen
import com.hypertrack.android.interactors.app.state.ConfirmEmailScreenView
import com.hypertrack.android.interactors.app.state.CurrentTripTab
import com.hypertrack.android.interactors.app.state.HistoryTab
import com.hypertrack.android.interactors.app.state.OrderDetailsScreen
import com.hypertrack.android.interactors.app.state.OrderDetailsScreenView
import com.hypertrack.android.interactors.app.state.OrdersTab
import com.hypertrack.android.interactors.app.state.OutageScreen
import com.hypertrack.android.interactors.app.state.OutageScreenView
import com.hypertrack.android.interactors.app.state.PermissionsScreen
import com.hypertrack.android.interactors.app.state.PermissionsScreenView
import com.hypertrack.android.interactors.app.state.PlaceDetailsScreen
import com.hypertrack.android.interactors.app.state.PlaceDetailsScreenView
import com.hypertrack.android.interactors.app.state.PlacesTab
import com.hypertrack.android.interactors.app.state.ProfileTab
import com.hypertrack.android.interactors.app.state.Screen
import com.hypertrack.android.interactors.app.state.SelectDestinationScreen
import com.hypertrack.android.interactors.app.state.SelectDestinationScreenView
import com.hypertrack.android.interactors.app.state.SendFeedbackScreen
import com.hypertrack.android.interactors.app.state.SendFeedbackScreenView
import com.hypertrack.android.interactors.app.state.SignInScreen
import com.hypertrack.android.interactors.app.state.SignInScreenView
import com.hypertrack.android.interactors.app.state.SplashScreen
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.SummaryTab
import com.hypertrack.android.interactors.app.state.TabsScreen
import com.hypertrack.android.interactors.app.state.TabsView
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.ui.screens.visits_management.tabs.history.Initial
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.withEffects
import java.time.LocalDate

class ScreensReducer(
    private val historyViewReducer: HistoryViewReducer
) {

    fun reduce(
        action: RegisterScreenAction,
        state: AppNotInitialized
    ): ReducerResult<AppState, AppEffect> {
        return when (action.screen) {
            is SplashScreen -> {
                state.copy(splashScreenViewState = SplashScreenView).withEffects()
            }
            else -> {
                state.withEffects(
                    ShowAndReportAppErrorEffect(IllegalActionException(action, state))
                )
            }
        }
    }

    fun reduce(
        action: RegisterScreenAction,
        state: AppInitialized
    ): ReducerResult<AppState, out AppEffect> {
        // todo avoid loading state (check the same screen in other way)
        val viewState = getInitialViewState(state, action.screen)
        return if (state.viewState::class != viewState::class) {
            viewState.withState {
                state.copy(viewState = it)
            }
        } else {
            // preserve old state if the opened screen is the same
            state.withEffects()
        }
    }

    private fun getInitialViewState(
        state: AppInitialized,
        screen: Screen
    ): ReducerResult<AppViewState, out AppEffect> {
        return when (screen) {
            AddGeotagScreen -> AddGeotagScreenView.withEffects()
            AddIntegrationScreen -> AddIntegrationScreenView.withEffects()
            AddOrderInfoScreen -> AddOrderInfoScreenView.withEffects()
            AddPlaceInfoScreen -> AddPlaceInfoScreenView.withEffects()
            AddPlaceScreen -> AddPlaceScreenView.withEffects()
            BackgroundPermissionsScreen -> BackgroundPermissionsScreenView.withEffects()
            ConfirmEmailScreen -> ConfirmEmailScreenView.withEffects()
            TabsScreen -> {
                val historyTabResult = getHistoryTabInitialState(screen, state)
                TabsView(
                    currentTripTab = CurrentTripTab,
                    historyTab = historyTabResult.newState,
                    ordersTab = OrdersTab,
                    placesTab = PlacesTab,
                    summaryTab = SummaryTab,
                    profileTab = ProfileTab
                ).withEffects(historyTabResult.effects)
            }
            OrderDetailsScreen -> OrderDetailsScreenView.withEffects()
            OutageScreen -> OutageScreenView.withEffects()
            PermissionsScreen -> PermissionsScreenView.withEffects()
            PlaceDetailsScreen -> PlaceDetailsScreenView.withEffects()
            SelectDestinationScreen -> SelectDestinationScreenView.withEffects()
            SendFeedbackScreen -> SendFeedbackScreenView.withEffects()
            SignInScreen -> SignInScreenView.withEffects()
            SplashScreen -> SplashScreenView.withEffects()
        }
    }

    private fun getHistoryTabInitialState(
        screen: Screen,
        state: AppInitialized
    ): ReducerResult<HistoryTab, out AppEffect> {
        val initial = Initial(LocalDate.now())
        return withLoggedIn(
            state,
            notLoggedIn = {
                initial.withEffects(
                    ShowAndReportAppErrorEffect(IllegalActionException(screen, state))
                )
            }
        ) { userLoggedIn ->
            historyViewReducer.map(userLoggedIn, userLoggedIn.history, initial).withEffects {
                val historyEffects = it.effects
                val startLoadingEffect = setOf(
                    AppActionEffect(
                        HistoryAppAction(
                            StartDayHistoryLoadingAction(
                                initial.date,
                                forceReloadIfLoading = true
                            )
                        )
                    )
                )
                historyEffects + startLoadingEffect
            }
        }.withState {
            HistoryTab(it)
        }
    }

    private fun <S, L : S, N : S> withLoggedIn(
        state: AppInitialized,
        notLoggedIn: () -> N,
        loggedIn: (UserLoggedIn) -> L
    ): S {
        return when (state.userState) {
            is UserLoggedIn -> loggedIn.invoke(state.userState)
            UserNotLoggedIn -> notLoggedIn.invoke()
        }
    }
}
