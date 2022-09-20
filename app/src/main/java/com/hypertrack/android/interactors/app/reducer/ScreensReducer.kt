package com.hypertrack.android.interactors.app.reducer

import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.HistoryAppAction
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.action.StartDayHistoryLoadingAction
import com.hypertrack.android.interactors.app.optics.AppStateOptics
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
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryScreenState
import com.hypertrack.android.ui.screens.visits_management.tabs.history.Initial
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.state_machine.mergeResults
import com.hypertrack.android.utils.withEffects
import java.time.LocalDate

class ScreensReducer(
    private val historyReducer: HistoryReducer,
    private val historyViewReducer: HistoryViewReducer
) {

    fun reduce(
        action: RegisterScreenAction,
        state: AppNotInitialized
    ): ReducerResult<out AppState, out AppEffect> {
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
    ): ReducerResult<out AppState, out AppEffect> {
        return if (state.viewState.isForScreen(action.screen)) {
            // preserve old state if the opened screen is the same
            state.withEffects()
        } else {
            initViewState(state, action.screen)
        }
    }

    private fun initViewState(
        state: AppInitialized,
        screen: Screen
    ): ReducerResult<out AppState, out AppEffect> {
        return when (screen) {
            AddGeotagScreen -> AddGeotagScreenView.toAppState(state).withEffects()
            AddIntegrationScreen -> AddIntegrationScreenView.toAppState(state).withEffects()
            AddOrderInfoScreen -> AddOrderInfoScreenView.toAppState(state).withEffects()
            AddPlaceInfoScreen -> AddPlaceInfoScreenView.toAppState(state).withEffects()
            AddPlaceScreen -> AddPlaceScreenView.toAppState(state).withEffects()
            BackgroundPermissionsScreen -> BackgroundPermissionsScreenView.toAppState(state)
                .withEffects()
            ConfirmEmailScreen -> ConfirmEmailScreenView.toAppState(state).withEffects()
            TabsScreen -> initTabsViewState(screen, state)
            OrderDetailsScreen -> OrderDetailsScreenView.toAppState(state).withEffects()
            OutageScreen -> OutageScreenView.toAppState(state).withEffects()
            PermissionsScreen -> PermissionsScreenView.toAppState(state).withEffects()
            PlaceDetailsScreen -> PlaceDetailsScreenView.toAppState(state).withEffects()
            SelectDestinationScreen -> SelectDestinationScreenView.toAppState(state).withEffects()
            SendFeedbackScreen -> SendFeedbackScreenView.toAppState(state).withEffects()
            SignInScreen -> SignInScreenView.toAppState(state).withEffects()
            SplashScreen -> SplashScreenView.toAppState(state).withEffects()
        }
    }

    private fun AppViewState.toAppState(oldState: AppInitialized): AppInitialized {
        return oldState.copy(viewState = this)
    }

    private fun initTabsViewState(
        screen: Screen,
        appState: AppInitialized
    ): ReducerResult<out AppState, out AppEffect> {
        return withLoggedIn(
            appState,
            notLoggedIn = {
                appState.withEffects(
                    ShowAndReportAppErrorEffect(IllegalActionException(screen, appState))
                )
            }
        ) { userLoggedIn ->
            // start loading history if needed
            val historyResult = historyReducer.reduce(
                HistoryAppAction(StartDayHistoryLoadingAction(LocalDate.now())),
                userLoggedIn,
                AppStateOptics.getHistorySubState(userLoggedIn, appState.viewState)
            )

            mergeResults(
                historyResult,
                otherResult = { historySubState ->
                    historyViewReducer.map(
                        userLoggedIn,
                        historySubState.newState.history,
                        Initial(LocalDate.now())
                    )
                }
            ) { historySubState, historyViewState ->
                historySubState.history to historyViewState
            }.withState {
                AppStateOptics.putHistorySubState(
                    appState,
                    userLoggedIn,
                    HistorySubState(it.first, it.second)
                ).copy(
                    viewState = TabsView(
                        historyTab = HistoryTab(it.second),
                    )
                )
            }
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
