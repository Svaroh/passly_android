package net.svaroh.passly.ui

import kotlinx.serialization.Serializable

/**
 * This class is used as data transfer object of account data (private key, user data, etc) which is injected via the
 * launch arguments.
 */
@Serializable
data class AccountSetupDataModel(
    val serverUserId: String,
    val domain: String,
    val userName: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val keyFingerprint: String,
    val armoredKey: String,
) {
    companion object {
        fun withRequiredValues(
            serverUserId: String,
            domain: String,
            armoredKey: String,
        ) = AccountSetupDataModel(
            serverUserId = serverUserId,
            domain = domain,
            armoredKey = armoredKey,
            firstName = "",
            lastName = "",
            avatarUrl = "",
            userName = "",
            keyFingerprint = "",
        )
    }
}
