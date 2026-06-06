package net.svaroh.passly.feature.authentication.auth

import net.svaroh.passly.core.idlingresource.SignInIdlingResource
import net.svaroh.passly.core.mvp.authentication.MfaProvidersHandler
import net.svaroh.passly.feature.authentication.auth.challenge.ChallengeDecryptor
import net.svaroh.passly.feature.authentication.auth.challenge.ChallengeProvider
import net.svaroh.passly.feature.authentication.auth.challenge.ChallengeVerifier
import net.svaroh.passly.feature.authentication.auth.challenge.MfaStatusProvider
import net.svaroh.passly.feature.authentication.auth.usecase.BiometryInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.FetchServerPublicPgpKeyUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.FetchServerPublicRsaKeyUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.GopenPgpTimeUpdater
import net.svaroh.passly.feature.authentication.auth.usecase.PostSignInActionsInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.RefreshSessionUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.RemoveAllAccountDataUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.RemoveServerPublicRsaKeyUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.SaveServerPublicRsaKeyUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.SignInUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.SignOutUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyPassphraseUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel

@Suppress("LongMethod")
fun Module.authModule() {
    viewModel { params ->
        AuthViewModel(
            authConfig = params.get(),
            userId = params.get(),
            appContext = params.get(),
            getAccountDataUseCase = get(),
            getPrivateKeyUseCase = get(),
            verifyPassphraseUseCase = get(),
            biometricCipher = get(),
            getPassphraseUseCase = get(),
            passphraseMemoryCache = get(),
            rootDetector = get(),
            biometryInteractor = get(),
            getGlobalPreferencesUseCase = get(),
            runtimeAuthenticatedFlag = get(),
            saveSessionUseCase = get(),
            saveSelectedAccountUseCase = get(),
            signOutUseCase = get(),
            saveServerFingerprintUseCase = get(),
            mfaStatusProvider = get(),
            getAndVerifyServerKeysInteractor = get(),
            signInVerifyInteractor = get(),
            inAppReviewInteractor = get(),
            signInIdlingResource = get(),
            postSignInActionsInteractor = get(),
            refreshSessionUseCase = get(),
            mfaProvidersHandler = get(),
        )
    }

    factoryOf(::MfaStatusProvider)
    factoryOf(::MfaProvidersHandler)

    singleOf(::FetchServerPublicPgpKeyUseCase)
    singleOf(::FetchServerPublicRsaKeyUseCase)
    singleOf(::SignInUseCase)
    singleOf(::ChallengeProvider)
    singleOf(::ChallengeDecryptor)
    singleOf(::ChallengeVerifier)
    singleOf(::VerifyPassphraseUseCase)
    singleOf(::GetAndVerifyServerKeysAndTimeInteractor)
    singleOf(::SignInVerifyInteractor)
    singleOf(::GopenPgpTimeUpdater)
    singleOf(::PostSignInActionsInteractor)
    singleOf(::RefreshSessionUseCase)
    singleOf(::SignOutUseCase)
    singleOf(::BiometryInteractor)
    singleOf(::SignInIdlingResource)
    singleOf(::SaveServerPublicRsaKeyUseCase)
    singleOf(::GetServerPublicRsaKeyUseCase)
    singleOf(::RemoveServerPublicRsaKeyUseCase)
    singleOf(::GetSessionExpiryUseCase)
    singleOf(::RemoveAllAccountDataUseCase)
}
