package com.hypertrack.android.utils.state_machine

import com.hypertrack.android.utils.ReducerResult

class ReducerResultWithViewStateEffect<S, E, VE : E>(
    newState: S,
    effects: Set<E>,
    viewEffect: VE
) : ReducerResult<S, E>(newState, effects + setOf(viewEffect))

fun <S : Any, E : Any, VE : E> S.withViewStateAndEffects(
    effects: Set<E>,
    viewEffect: VE
): ReducerResultWithViewStateEffect<S, E, VE> {
    return ReducerResultWithViewStateEffect(
        this,
        effects,
        viewEffect
    )
}
