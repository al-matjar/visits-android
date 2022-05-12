package com.hypertrack.android.use_case.handle_push

class UnknownPushNotificationException(data: Map<String, String>) : Exception(data.toString())
