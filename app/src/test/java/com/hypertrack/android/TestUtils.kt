package com.hypertrack.android

import androidx.lifecycle.LiveData
import junit.framework.AssertionFailedError
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlin.reflect.KClass

fun <T> LiveData<T>.observeAndGetValue(): T {
    observeForever {}
    return value!!
}

fun <T> LiveData<T>.observeAndAssertNull() {
    observeForever {}
    return assertNull(value)
}

fun Set<Any>.assertEffects(vararg classes: KClass<*>) {
    if (classes.size != size) {
        throw AssertionFailedError("${classes.size} effects are expected but got $this")
    }
    assertEquals(classes.size, size)
    classes.forEach {
        if (filterIsInstance(it.java).size != 1) {
            throw AssertionFailedError("${it.simpleName} is expected but not found in $this")
        }
    }
}

fun Set<Any>.assertEffect(clazz: KClass<*>) {
    if (filterIsInstance(clazz.java).size != 1) {
        throw AssertionFailedError("${clazz.simpleName} is expected but not found")
    }
}

fun Set<Any>.assertNoEffects() {
    if (size != 0) {
        throw AssertionFailedError("No effects are expected but found $this")
    }
}

fun Set<Any>.assertWithChecks(vararg checks: (Set<Any>) -> Unit) {
    if (size != checks.size) {
        throw AssertionFailedError("Expected ${checks.size} effects but got $this")
    }
    checks.forEach { it.invoke(this) }
}

inline fun <reified T> createEffectCheck(crossinline check: (T) -> Boolean): (Set<Any>) -> Unit {
    return {
        val error = "Effect ${T::class.simpleName} not found in $it"
        if (it.isEmpty()) throw AssertionFailedError(error)
        val element = it.filterIsInstance<T>().firstOrNull()
        element?.let(check) ?: throw AssertionFailedError(error)
    }
}
