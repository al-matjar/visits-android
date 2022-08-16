package com.hypertrack.android.models.local

enum class OrderStatus(val value: String) {
    ONGOING("ongoing"),
    COMPLETED("completed"),
    CANCELED("cancelled"),
    SNOOZED("snoozed"),
    UNKNOWN("");

    companion object {
        fun fromString(str: String?): OrderStatus {
            for (i in values()) {
                if (str == i.value) {
                    return i
                }
            }
            return UNKNOWN
        }
    }
}
