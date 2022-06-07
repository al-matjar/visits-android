package com.hypertrack.android.di

import android.app.Application
import android.util.Log
import com.hypertrack.android.interactors.app.InitAppAction
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.NotInitialized
import com.hypertrack.android.interactors.app.Initialized
import com.hypertrack.android.interactors.app.TrackingStateChangedAction
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.interactors.app.UserNotLoggedIn
import com.hypertrack.android.ui.activity.ActivityViewModelFactory
import com.hypertrack.android.ui.common.ParamViewModelFactory
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.UserScopeViewModelFactory
import com.hypertrack.android.ui.common.ViewModelFactory
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.use_case.app.AppCreationUseCase
import com.hypertrack.android.use_case.sdk.NewTrackingState
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.FirebaseCrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.TrackingState

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
object Injector {

    //should be static to enable reliable exception reporting in all places in the code
    lateinit var crashReportsProvider: CrashReportsProvider
    private lateinit var resourceProvider: ResourceProvider

    // todo remove and refactor
    // legacy tracking state events callback, needed for HyperTrackService
    // (some classes are dependent on it)
    // lazy init because crashReportsProvider not available before app creation
    private val trackingState by lazy {
        TrackingState(crashReportsProvider)
    }

    private val trackingStateListener = { trackingState: NewTrackingState ->
        crashReportsProvider.log(trackingState.toString())
        appInteractor.handleAction(TrackingStateChangedAction(trackingState))
    }.also {
        Log.v("trackingStateListener", it.toString())
    }

    private lateinit var appInteractor: AppInteractor

    fun appOnCreate(application: Application) {
        crashReportsProvider = FirebaseCrashReportsProvider(application)
        resourceProvider = OsUtilsProvider(application, crashReportsProvider)
        val appScope = AppCreationUseCase().execute(
            application,
            crashReportsProvider,
            trackingState,
            trackingStateListener
        )
        appInteractor = AppInteractor(appScope)
        appInteractor.handleAction(InitAppAction(application))
    }

    fun provideActivityViewModelFactory(): ActivityViewModelFactory {
        return when (val state = appInteractor.appState.requireValue()) {
            is NotInitialized -> {
                ActivityViewModelFactory(
                    appInteractor,
                    state.appScope,
                    state.useCases
                )
            }
            is Initialized -> {
                ActivityViewModelFactory(
                    appInteractor,
                    state.appScope,
                    state.useCases,
                )
            }
        }
    }

    fun provideViewModelFactory(): ViewModelFactory {
        return when (val state = appInteractor.appState.requireValue()) {
            is NotInitialized -> {
                ViewModelFactory(appInteractor, state.appScope, state.useCases)
            }
            is Initialized -> {
                ViewModelFactory(appInteractor, state.appScope, state.useCases)
            }
        }
    }

    // todo remove (separate factory for each vm)
    fun <T> provideUserScopeParamViewModelFactory(param: T): ParamViewModelFactory<T> {
        return when (val state = appInteractor.appState.requireValue()) {
            is NotInitialized -> {
                throw IllegalStateException("App is not initialized")
            }
            is Initialized -> {
                when (state.userState) {
                    is UserLoggedIn -> {
                        ParamViewModelFactory(
                            param,
                            appInteractor,
                            state.appScope,
                            state.userState.userScope,
                        )
                    }
                    UserNotLoggedIn -> {
                        throw IllegalStateException("User is not logged in")
                    }
                }
            }
        }
    }

    fun provideUserScopeViewModelFactory(): UserScopeViewModelFactory {
        return when (val state = appInteractor.appState.requireValue()) {
            is NotInitialized -> {
                throw IllegalStateException("App is not initialized")
            }
            is Initialized -> {
                when (state.userState) {
                    is UserLoggedIn -> {
                        state.userState.userScope.userScopeViewModelFactory
                    }
                    UserNotLoggedIn -> {
                        throw IllegalStateException("User is not logged in")
                    }
                }
            }
        }
    }

    fun provideTabs(): List<Tab> = mutableListOf<Tab>().apply {
        addAll(
            listOf(
                Tab.CURRENT_TRIP,
                Tab.HISTORY,
            )
        )
        add(Tab.ORDERS)
        addAll(
            listOf(
                Tab.PLACES,
                Tab.SUMMARY,
                Tab.PROFILE,
            )
        )
    }

    fun provideAppInteractor(): AppInteractor {
        return appInteractor
    }

}

