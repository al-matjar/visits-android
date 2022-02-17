package com.hypertrack.android.repository.user

import com.hypertrack.android.repository.preferences.PreferencesRepository
import com.hypertrack.android.utils.KeyValueEntry

class UserRepository(
    private val preferencesRepository: PreferencesRepository
) {
    val userData: KeyValueEntry<UserData> = preferencesRepository.userData
}


