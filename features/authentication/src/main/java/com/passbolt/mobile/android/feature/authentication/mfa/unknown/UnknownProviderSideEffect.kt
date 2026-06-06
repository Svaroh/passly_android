package net.svaroh.passly.feature.authentication.mfa.unknown

sealed interface UnknownProviderSideEffect {
    data object CloseAndNavigateToStartup : UnknownProviderSideEffect
}
