package com.hypertrack.android.use_case.handle_push

import com.hypertrack.android.utils.exception.BaseException

class UnknownPushNotificationException(data: Map<String, String>) : BaseException(data.toString())
