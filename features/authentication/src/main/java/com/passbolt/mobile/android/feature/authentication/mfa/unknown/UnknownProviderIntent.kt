package net.svaroh.passly.feature.authentication.mfa.unknown

sealed interface UnknownProviderIntent {
    data object Close : UnknownProviderIntent
}
