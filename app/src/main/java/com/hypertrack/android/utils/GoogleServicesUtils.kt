package com.hypertrack.android.utils

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.google.android.play.core.tasks.Task as PlayTask

suspend fun <T : Any> Task<T>?.toSuspendCoroutine(): Result<T> = suspendCoroutine { continuation ->
    if (this != null) {
        addOnSuccessListener {
            continuation.resume(it.asSuccess())
        }
        addOnFailureListener {
            continuation.resume(it.asFailure())
        }
    } else {
        continuation.resume(NullPointerException("Task is null").asFailure())
    }
}

suspend fun <T> PlayTask<T>?.toSuspendCoroutine(): Result<T> = suspendCoroutine { continuation ->
    if (this != null) {
        addOnSuccessListener {
            continuation.resume(it.asSuccess())
        }
        addOnFailureListener {
            continuation.resume(it.asFailure())
        }
    } else {
        continuation.resume(NullPointerException("Task is null").asFailure())
    }
}
