package net.svaroh.passly.feature.authentication.auth.usecase

import net.svaroh.passly.domain.rbac.usecase.RbacInteractor
import net.svaroh.passly.entity.featureflags.FeatureFlagsModel
import net.svaroh.passly.featureflags.usecase.FeatureFlagsInteractor
import timber.log.Timber

class PostSignInActionsInteractor(
    private val featureFlagsInteractor: FeatureFlagsInteractor,
    private val rbacInteractor: RbacInteractor,
) {
    suspend fun launchPostSignInActions(
        onError: (Error) -> Unit,
        onSuccess: suspend () -> Unit,
    ) {
        Timber.d("Fetching feature flags")
        when (val featureFlagsResult = featureFlagsInteractor.fetchAndSaveFeatureFlags()) {
            is FeatureFlagsInteractor.Output.Success -> {
                Timber.d("Feature flags fetched")
                if (processRbac(featureFlagsResult.featureFlags, onError)) {
                    onSuccess()
                }
            }
            is FeatureFlagsInteractor.Output.Failure -> {
                Timber.e("Failed to fetch feature flags")
                onError(Error.ConfigurationFetchError)
            }
        }
    }

    private suspend fun processRbac(
        featureFlagsModel: FeatureFlagsModel,
        onError: (Error) -> Unit,
    ): Boolean =
        if (featureFlagsModel.isRbacAvailable) {
            Timber.d("RBAC available, fetching RBAC")
            when (rbacInteractor.fetchAndSaveRbacRulesFlags()) {
                is RbacInteractor.Output.Failure -> {
                    Timber.e("Failed to fetch RBAC")
                    onError(Error.ConfigurationFetchError)
                    false
                }
                is RbacInteractor.Output.Success -> true
            }
        } else {
            Timber.d("RBAC not available")
            true
        }

    sealed class Error {
        data object ConfigurationFetchError : Error()
    }
}
