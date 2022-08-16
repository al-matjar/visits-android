package com.hypertrack.android.models.local

enum class TripStatus(val value: String) {
    ACTIVE("active"),
    COMPLETED("completed"),
    PROGRESSING_COMPLETION("processing_completion"),
    UNKNOWN("");

    companion object {
        fun fromString(str: String?): TripStatus {
            for (i in values()) {
                if (str == i.value) {
                    return i
                }
            }
            return UNKNOWN
        }
    }
}
