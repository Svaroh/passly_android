package net.svaroh.passly.feature.authentication

import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig
import net.svaroh.passly.domain.accounts.usecase.GetAccountDataUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.accounts.usecase.SaveCurrentApiUrlUseCase

class AuthenticationStartUpResolver(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
) {
    data class Result(
        val skipAccountsList: Boolean,
        val initialUserId: String?,
    )

    fun resolve(
        authConfig: AuthConfig,
        userId: String?,
    ): Result {
        val currentAccount = userId ?: getSelectedAccountUseCase.execute(Unit).selectedAccount
        val skipAccountsList = authConfig is AuthConfig.Setup && currentAccount != null

        if (!skipAccountsList && authConfig !is AuthConfig.ManageAccount && currentAccount != null) {
            val account = getAccountDataUseCase.execute(UserIdInput(currentAccount))
            saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(account.url))
        }

        return Result(
            skipAccountsList = skipAccountsList,
            initialUserId = if (authConfig !is AuthConfig.ManageAccount) currentAccount else null,
        )
    }
}
