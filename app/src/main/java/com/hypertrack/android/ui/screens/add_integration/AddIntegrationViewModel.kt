package com.hypertrack.android.ui.screens.add_integration

import android.view.View
import androidx.lifecycle.*
import com.hypertrack.android.interactors.app.AppEffectAction
import com.hypertrack.android.interactors.app.ReportAppErrorEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.noAction
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.models.Integration
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.ui.common.util.updateAsFlow
import com.hypertrack.android.ui.common.util.updateConsumableAsFlow
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.StateMachine
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asSet
import com.hypertrack.android.utils.catchException
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.withEffects
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_integration.view.tvDescription
import kotlinx.android.synthetic.main.item_integration.view.tvName
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class AddIntegrationViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val userStateFlow: StateFlow<UserLoggedIn?>,
    private val integrationsRepository: IntegrationsRepository
) : BaseViewModel(baseDependencies) {

    val adapter = createAdapter()
    val viewState = MutableLiveData<ViewState>()

    val integrationSelectedEvent = MutableLiveData<Consumable<Integration>>()

    private val stateMachine = StateMachine<Action, State, Effect>(
        javaClass.simpleName,
        crashReportsProvider,
        Loading(query = EMPTY_QUERY),
        actionsScope,
        this::reduce,
        this::applyEffects,
        this::stateChangeEffects,
    )

    fun handleAction(action: Action) {
        stateMachine.handleAction(action)
    }

    private fun reduce(state: State, action: Action): ReducerResult<out State, out Effect> {
        return userStateFlow.value?.let {
            reduceIfLoggedIn(action, state, it)
        } ?: IllegalActionException(action, state).let { exception ->
            state.withEffects(
                AppEffect(ShowAndReportAppErrorEffect(exception))
            )
        }
    }

    private fun reduceIfLoggedIn(
        action: Action,
        state: State,
        userState: UserLoggedIn
    ): ReducerResult<out State, out Effect> {
        return when (state) {
            is Loading -> {
                when (action) {
                    is InitAction -> {
                        reduce(action)
                    }
                    is OnIntegrationsResultAction -> {
                        when (action.result) {
                            is Success -> {
                                Loaded(
                                    query = state.query,
                                    integrations = action.result.data.items,
                                    nextPageToken = action.result.data.paginationToken,
                                    newQuery = state.query
                                ).withEffects()
                            }
                            is Failure -> {
                                onLoadingError(action.result.exception)
                            }
                        }
                    }
                    is OnQueryChangedAction,
                    OnRefreshAction,
                    is OnIntegrationClickedAction,
                    OnLoadMoreAction,
                    OnInitSearchAction -> {
                        illegalAction(action, state)
                    }
                }
            }
            is Loaded -> {
                when (action) {
                    is InitAction -> {
                        reduce(action)
                    }
                    is OnQueryChangedAction -> {
                        state.copy(newQuery = action.query).withEffects()
                    }
                    OnInitSearchAction -> {
                        Loading(query = state.newQuery).withEffects(
                            LoadIntegrationsEffect(
                                query = state.newQuery,
                                pageToken = null
                            )
                        )
                    }
                    OnLoadMoreAction -> {
                        state.withEffects(
                            state.nextPageToken?.let { pageToken ->
                                LoadIntegrationsEffect(
                                    query = state.query,
                                    pageToken = pageToken
                                ).asSet()
                            } ?: setOf()
                        )
                    }
                    is OnIntegrationsResultAction -> {
                        when (action.result) {
                            is Success -> {
                                state.copy(
                                    integrations = state.integrations + action.result.data.items,
                                    nextPageToken = action.result.data.paginationToken
                                ).withEffects()
                            }
                            is Failure -> {
                                onLoadingError(action.result.exception)
                            }
                        }
                    }
                    is OnIntegrationClickedAction -> {
                        state.withEffects(
                            OnIntegrationSelectedEffect(action.integration)
                        )
                    }
                    OnRefreshAction -> {
                        reduceIfLoggedIn(OnInitSearchAction, state, userState)
                    }
                }
            }
        }
    }

    private fun reduce(action: InitAction): ReducerResult<Loading, LoadIntegrationsEffect> {
        return Loading(query = EMPTY_QUERY).withEffects(
            LoadIntegrationsEffect(query = EMPTY_QUERY, pageToken = null)
        )
    }

    private fun applyEffects(effects: Set<Effect>) {
        effects.forEach { effect ->
            runInVmEffectsScope {
                getEffectFlow(effect)
                    .catchException { onError(it) }
                    .collect {
                        it?.let { handleAction(it) }
                    }
            }
        }
    }

    private fun stateChangeEffects(newState: State): Set<Effect> {
        return setOf(UpdateViewStateEffect(newState, getViewState(newState)))
    }

    private fun onLoadingError(exception: Exception): ReducerResult<State, AppEffect> {
        return Loaded(
            query = EMPTY_QUERY,
            integrations = listOf(),
            nextPageToken = null,
            newQuery = EMPTY_QUERY
        ).withEffects(
            AppEffect(ShowAndReportAppErrorEffect(exception))
        )
    }

    private fun getEffectFlow(effect: Effect): Flow<Action?> {
        return when (effect) {
            is AppEffect -> {
                appInteractor.handleActionFlow(AppEffectAction(effect.appEffect)).noAction()
            }
            is LoadIntegrationsEffect -> {
                suspend {
                    integrationsRepository.getIntegrations(effect.query).let {
                        OnIntegrationsResultAction(it)
                    }
                }.asFlow()
            }
            is OnIntegrationSelectedEffect -> {
                integrationSelectedEvent.updateConsumableAsFlow(effect.integration).noAction()
            }
            is UpdateViewStateEffect -> {
                viewState.updateAsFlow(effect.viewState).map {
                    adapter.updateItems(effect.state.getIntegrationsIfPresent())
                }.flowOn(Dispatchers.Main).noAction()
            }
        }
    }

    private fun getViewState(state: State): ViewState {
        return when (state) {
            is Loading -> {
                ViewState(
                    showProgressbar = true,
                    showList = false,
                    showPlaceholder = false,
                    showSearchButton = false,
                    showSearchField = false
                )
            }
            is Loaded -> {
                ViewState(
                    showProgressbar = false,
                    showList = state.integrations.isNotEmpty() && state.newQuery == state.query,
                    showPlaceholder = state.integrations.isEmpty(),
                    showSearchButton = state.newQuery != state.query,
                    showSearchField = true
                )
            }
        }
    }

    private fun illegalAction(action: Action, state: State): ReducerResult<State, Effect> {
        return IllegalActionException(action, state).let {
            if (MyApplication.DEBUG_MODE) {
                throw it
            } else {
                state.withEffects(AppEffect(ReportAppErrorEffect(it)))
            }
        }
    }

    private fun createAdapter(): BaseAdapter<Integration, BaseAdapter.BaseVh<Integration>> {
        return object : BaseAdapter<Integration, BaseAdapter.BaseVh<Integration>>() {
            override val itemLayoutResource = R.layout.item_integration

            override fun createViewHolder(
                view: View,
                baseClickListener: (Int) -> Unit
            ): BaseVh<Integration> {
                return object : BaseContainerVh<Integration>(view, baseClickListener) {
                    override fun bind(item: Integration) {
                        item.name?.toView(containerView.tvName)
                        item.id.toView(containerView.tvDescription)
                    }
                }
            }
        }.apply {
            onItemClickListener = {
                handleAction(OnIntegrationClickedAction(it))
            }
        }
    }

    companion object {
        const val EMPTY_QUERY = ""
    }

}
