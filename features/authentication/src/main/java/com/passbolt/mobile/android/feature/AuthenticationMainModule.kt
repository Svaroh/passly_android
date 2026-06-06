package net.svaroh.passly.feature

import androidx.biometric.BiometricPrompt
import net.svaroh.passly.core.navigation.compose.base.Feature
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.feature.authentication.AuthenticationStartUpResolver
import net.svaroh.passly.feature.authentication.accountslist.accountsListModule
import net.svaroh.passly.feature.authentication.auth.authModule
import net.svaroh.passly.feature.authentication.auth.usecase.RefreshSessionUseCase
import net.svaroh.passly.feature.authentication.mfa.duo.authWithDuoModule
import net.svaroh.passly.feature.authentication.mfa.totp.enterTotpModule
import net.svaroh.passly.feature.authentication.mfa.unknown.unknownProviderModule
import net.svaroh.passly.feature.authentication.mfa.yubikey.scanYubikeyModule
import net.svaroh.passly.feature.authentication.navigation.AuthenticationFeatureNavigation
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authenticationModule =
    module {
        single<FeatureModuleNavigation>(named(Feature.AUTHENTICATION)) {
            AuthenticationFeatureNavigation()
        }
        single { BiometricPrompt.PromptInfo.Builder() }
        factoryOf(::RefreshSessionUseCase)
        factoryOf(::AuthenticationStartUpResolver)

        accountsListModule()
        authModule()
        scanYubikeyModule()
        enterTotpModule()
        unknownProviderModule()
        authWithDuoModule()
    }
