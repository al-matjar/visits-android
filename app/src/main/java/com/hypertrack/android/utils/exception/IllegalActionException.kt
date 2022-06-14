package com.hypertrack.android.utils.exception

class IllegalActionException(action: Any, state: Any) :
    IllegalStateException("Illegal action $action for state $state")
