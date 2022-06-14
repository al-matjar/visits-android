package com.hypertrack.android.deeplink

class BranchErrorException(
    code: Int,
    branchMessage: String
) : Exception("$code: $branchMessage")
