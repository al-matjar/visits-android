package com.hypertrack.android.models.local


/**
 * This type is required to guarantee that publishable key
 * will not be mocked in some places
 * */
class RealPublishableKey(publishableKey: String) : PublishableKey(publishableKey)

open class PublishableKey(val value: String) {
    override fun toString(): String {
        return "${javaClass.simpleName}(value=$value)"
    }
}

