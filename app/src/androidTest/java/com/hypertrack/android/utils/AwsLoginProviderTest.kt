package com.hypertrack.android.utils

import androidx.test.platform.app.InstrumentationRegistry
import com.hypertrack.android.use_case.app.AppCreationUseCase.Companion.LIVE_API_URL_BASE
import com.hypertrack.logistics.android.github.R
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AwsLoginProviderTest {

    val ctx by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    @Test
    fun itShouldGetTokenByNameAndPassword() {
        val accountLoginProvider: CognitoAccountLoginProvider =
            CognitoAccountLoginProviderImpl(ctx)
        val login = ctx.resources.getString(R.string.awsLoginTestUserName)
        val pwd = ctx.resources.getString(R.string.awsLoginTestUserPwd)
        val expected = ctx.resources.getString(R.string.awsLoginTestExpectedPk)
        runBlocking {
            accountLoginProvider.awsInitCallWrapper()
            val res = accountLoginProvider.awsLoginCallWrapper(login, pwd) as AwsSignInSuccess
            val tokenRes = accountLoginProvider.awsTokenCallWrapper() as CognitoToken
            val pk = cognitoExchangeTokenApi.getPublishableKey(tokenRes.token)
                .body()!!.publishableKey!!
            assertEquals(expected, pk)
        }
    }

    private val cognitoExchangeTokenApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(LIVE_API_URL_BASE)
            .addConverterFactory(MoshiConverterFactory.create(TestInjector.getMoshi()))
            .build()
        return@lazy retrofit.create(CognitoExchangeTokenApi::class.java)
    }

    companion object {
        const val TAG = "AwsLoginProviderTest"
    }
}
