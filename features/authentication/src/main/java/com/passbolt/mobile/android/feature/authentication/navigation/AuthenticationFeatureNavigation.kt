package net.svaroh.passly.feature.authentication.navigation

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.LocalAuthenticationParams
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.AuthenticationNavigationKey.AccountsList
import net.svaroh.passly.core.navigation.compose.keys.AuthenticationNavigationKey.Auth
import net.svaroh.passly.core.navigation.compose.keys.AuthenticationNavigationKey.MfaDuo
import net.svaroh.passly.core.navigation.compose.keys.AuthenticationNavigationKey.MfaTotp
import net.svaroh.passly.core.navigation.compose.keys.AuthenticationNavigationKey.MfaUnknownProvider
import net.svaroh.passly.core.navigation.compose.keys.AuthenticationNavigationKey.MfaYubikey
import net.svaroh.passly.core.navigation.compose.results.NavigationResultEventBus
import net.svaroh.passly.feature.authentication.accountslist.AccountsListScreen
import net.svaroh.passly.feature.authentication.auth.AuthScreen
import net.svaroh.passly.feature.authentication.mfa.MfaDialogState
import net.svaroh.passly.feature.authentication.mfa.MfaResult
import net.svaroh.passly.feature.authentication.mfa.duo.AuthWithDuoScreen
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpScreen
import net.svaroh.passly.feature.authentication.mfa.unknown.UnknownProviderScreen
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyScreen
import org.koin.compose.koinInject

class AuthenticationFeatureNavigation : FeatureModuleNavigation {
    @Suppress("LongMethod")
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<AccountsList> {
                val params = LocalAuthenticationParams.current
                PassboltTheme {
                    AccountsListScreen(
                        authConfig = params.authConfig,
                    )
                }
            }

            entry<Auth> { key ->
                val params = LocalAuthenticationParams.current

                PassboltTheme {
                    AuthScreen(
                        authConfig = key.authConfig ?: params.authConfig,
                        userId = key.userId,
                        appContext = params.appContext,
                    )
                }
            }

            entry<MfaTotp> { key ->
                val resultBus = NavigationResultEventBus.current
                val navigator: AppNavigator = koinInject()
                PassboltTheme {
                    EnterTotpScreen(
                        mfaState = MfaDialogState.Totp(key.authToken, key.hasOtherProviders),
                        onMfaResult = {
                            resultBus.sendResult<MfaResult>(result = it)
                            navigator.navigateBack()
                        },
                    )
                }
            }

            entry<MfaYubikey> { key ->
                val resultBus = NavigationResultEventBus.current
                val navigator: AppNavigator = koinInject()
                PassboltTheme {
                    ScanYubikeyScreen(
                        mfaState = MfaDialogState.Yubikey(key.authToken, key.hasOtherProviders),
                        onMfaResult = {
                            resultBus.sendResult<MfaResult>(result = it)
                            navigator.navigateBack()
                        },
                    )
                }
            }

            entry<MfaDuo> { key ->
                val resultBus = NavigationResultEventBus.current
                val navigator: AppNavigator = koinInject()
                PassboltTheme {
                    AuthWithDuoScreen(
                        mfaState = MfaDialogState.Duo(key.authToken, key.hasOtherProviders),
                        onMfaResult = {
                            resultBus.sendResult<MfaResult>(result = it)
                            navigator.navigateBack()
                        },
                    )
                }
            }

            entry<MfaUnknownProvider> {
                PassboltTheme {
                    UnknownProviderScreen()
                }
            }
        }
}
