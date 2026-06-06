package net.svaroh.passly.accountinit

import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.accounts.GetAccountsUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.RemoveAllAccountDataUseCase
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class AccountDataCleaner(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
) : KoinComponent {
    fun clearAccountData() {
        runBlocking {
            get<GetAccountsUseCase>().execute(Unit).users.forEach {
                get<RemoveAllAccountDataUseCase>().execute(UserIdInput(it))
            }
        }
    }
}
