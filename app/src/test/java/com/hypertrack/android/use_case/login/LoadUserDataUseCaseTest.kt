package com.hypertrack.android.use_case.login

import com.hypertrack.android.repository.preferences.SharedPreferencesStringEntry
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.repository.user.UserRepository
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class LoadUserDataUseCaseTest {

    @Test
    fun `is should delete user scope data on user data null for LoggedIn state`() {
        val deleteUserScopeDataUseCase = mockk<DeleteUserScopeDataUseCase>() {
            every { execute() } returns flowOf(Unit)
        }
        runBlocking {
            loadUserDataUseCase(
                userRepository = mockk {
                    every { userData } returns mockk {
                        every { load() } returns Success(null)
                    }
                },
                deleteUserScopeDataUseCase = deleteUserScopeDataUseCase
            ).execute().collect()
        }
    }

    companion object {
        fun loadUserDataUseCase(
            deleteUserScopeDataUseCase: DeleteUserScopeDataUseCase,
            userRepository: UserRepository
        ): LoadUserDataUseCase {
            return LoadUserDataUseCase(
                userRepository,
                deleteUserScopeDataUseCase
            )
        }
    }

}
