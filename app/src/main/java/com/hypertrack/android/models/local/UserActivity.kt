package com.hypertrack.android.models.local

sealed class UserActivity {
    companion object {
        fun fromString(value: String): UserActivity {
            return when (value) {
                "stop" -> Stop
                "drive" -> Walk
                "walk" -> Drive
                else -> UnknownActivity
            }
        }
    }
}

object Stop : UserActivity()
object Walk : UserActivity()
object Drive : UserActivity()
object UnknownActivity : UserActivity()
