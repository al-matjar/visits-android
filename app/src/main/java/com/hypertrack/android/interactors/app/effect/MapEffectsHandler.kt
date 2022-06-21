package com.hypertrack.android.interactors.app.effect

import com.google.android.gms.maps.CameraUpdateFactory
import com.hypertrack.android.interactors.app.AppAction
import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.AppMapEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.noAction
import com.hypertrack.android.use_case.error.LogExceptionIfFailureUseCase
import com.hypertrack.android.use_case.map.ClearMapUseCase
import com.hypertrack.android.use_case.map.UpdateMapDataUseCase
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE")
class MapEffectsHandler(
    private val logExceptionIfFailureUseCase: LogExceptionIfFailureUseCase,
    private val getAppEffectFlow: (AppEffect) -> Flow<AppAction?>
) {
    private val clearMapUseCase: ClearMapUseCase = ClearMapUseCase()
    private val updateMapDataUseCase: UpdateMapDataUseCase = UpdateMapDataUseCase()

    fun applyEffect(appEffect: AppMapEffect): Flow<AppAction?> {
        val effect = appEffect.mapEffect
        return when (effect) {
            is ClearMapEffect -> {
                clearMapUseCase.execute(effect.map).map { null }
            }
            is MoveMapToBoundsEffect -> {
                {
                    effect.map.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            effect.latLngBounds,
                            effect.mapPadding
                        )
                    )
                }.asFlow().flowOn(Dispatchers.Main).noAction()
            }
            is MoveMapToLocationEffect -> {
                {
                    effect.map.moveCamera(
                        CameraUpdateFactory.newLatLng(effect.latLng)
                    )
                }.asFlow().flowOn(Dispatchers.Main).noAction()
            }
            is UpdateMapEffect -> {
                updateMapDataUseCase.execute(effect.map, effect.data)
                    .flowOn(Dispatchers.Main)
                    .showAndReportErrorIfFailure()
            }
        }
    }

    private fun Flow<Result<Unit>>.showAndReportErrorIfFailure(): Flow<AppAction?> {
        return flatMapConcat {
            when (it) {
                is Success -> flowOf(Unit).noAction()
                is Failure -> getAppEffectFlow.invoke(ShowAndReportAppErrorEffect(it.exception))
            }
        }
    }

}
