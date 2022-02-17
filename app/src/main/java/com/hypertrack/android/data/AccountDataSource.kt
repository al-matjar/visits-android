package com.hypertrack.android.data

import com.hypertrack.android.repository.*

interface AccountDataStorage {

    fun getAccountData(): AccountData
    fun saveAccountData(accountData: AccountData)

}
