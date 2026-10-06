package net.svaroh.passly.accountinit

import kotlinx.coroutines.runBlocking
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.domain.accounts.usecase.GetAccountsUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.RemoveAllAccountDataUseCase
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
