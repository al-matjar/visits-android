package com.hypertrack.android.use_case.deeplink

import com.hypertrack.android.ui.screens.sign_in.use_case.result.BranchReferrer
import com.hypertrack.android.ui.screens.sign_in.use_case.result.InvalidDeeplink
import com.hypertrack.android.ui.screens.sign_in.use_case.result.ValidDeeplink
import com.hypertrack.android.ui.screens.sign_in.use_case.result.ValidateDeeplinkUrlResult
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.flatMapSuccess
import com.hypertrack.android.utils.mapSuccess
import com.hypertrack.android.utils.toFlow
import com.hypertrack.android.utils.tryAsResult
import kotlinx.coroutines.flow.Flow
import org.jetbrains.annotations.TestOnly
import java.util.regex.Pattern

class ValidateDeeplinkUrlUseCase(
    private val deeplinkRegex: Pattern
) {

    fun execute(url: String): Flow<Result<ValidateDeeplinkUrlResult>> {
        return tryAsResult {
            with(deeplinkRegex.matcher(url)) {
                matches()
            }
        }.toFlow().mapSuccess { isHyperTrackDeeplink ->
            when {
                isHyperTrackDeeplink -> ValidDeeplink(url)
                url.startsWith(BRANCH_REFERRER_PREFIX) -> BranchReferrer(url)
                else -> InvalidDeeplink(url)
            }
        }
    }

    @Suppress("RegExpRedundantEscape")
    companion object {
        public val BRANCH_REFERRER_PREFIX = "hypertrack-logistics://open?_branch_referrer="

        public fun createDeeplinkRegex(): Pattern {
            return Pattern
                .compile("https:\\/\\/hypertrack-logistics\\.app\\.link\\/(.+)(\\?.*)?")
        }
    }

}
