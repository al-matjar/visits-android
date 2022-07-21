package com.hypertrack.android.utils

import java.time.LocalDate
import java.util.*
import kotlin.Comparator
import kotlin.collections.map as mapKotlin

open class EnforceSortedKeyMap<K : Comparable<K>, V, C : Comparator<K>>(
    unsortedData: Map<K, V>,
    comparator: C
) {
    val data = unsortedData.toSortedMap(comparator)
}

object ReverseDateComparator : Comparator<LocalDate> {
    @Suppress("UNCHECKED_CAST")
    override fun compare(o1: LocalDate?, o2: LocalDate?): Int =
        (ReverseOrderComparator as Comparator<LocalDate>).compare(o1, o2)
}

//copy of kotlin.comparisons.ReverseOrderComparator
object ReverseOrderComparator : Comparator<Comparable<Any>> {
    override fun compare(a: Comparable<Any>, b: Comparable<Any>): Int = b.compareTo(a)

    @Suppress("VIRTUAL_MEMBER_HIDDEN")
    fun reversed(): Comparator<Comparable<Any>> = NaturalOrderComparator
}

//copy of kotlin.comparisons.NaturalOrderComparator
object NaturalOrderComparator : Comparator<Comparable<Any>> {
    override fun compare(a: Comparable<Any>, b: Comparable<Any>): Int = a.compareTo(b)

    @Suppress("VIRTUAL_MEMBER_HIDDEN")
    fun reversed(): Comparator<Comparable<Any>> = ReverseOrderComparator
}

fun <T, E> Set<T>.map(mapper: (T) -> E): Set<E> {
    return mapKotlin(mapper).toSet()
}
