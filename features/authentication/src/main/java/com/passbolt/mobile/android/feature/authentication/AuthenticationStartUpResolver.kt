package net.svaroh.passly.feature.authentication

import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.accountdata.GetAccountDataUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig

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
