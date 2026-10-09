package net.svaroh.passly.data.auth.datasource.local

internal object Constants {
    const val ACCESS_TOKEN_KEY = "ACCESS_TOKEN_KEY"
    const val REFRESH_TOKEN_KEY = "SESSION_REFRESH_TOKEN_KEY"
    const val MFA_TOKEN_KEY = "REFRESH_TOKEN_KEY"

    // TODO bug in the file name - migrate to separate prefs file
    const val RESOURCE_DATABASE_ALIAS = "current_url"
    const val DATABASE_PASSPHRASE_KEY = "passphrase_key"
}
