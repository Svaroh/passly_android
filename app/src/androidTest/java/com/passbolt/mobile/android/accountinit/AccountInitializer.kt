package net.svaroh.passly.accountinit

import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.account.SaveAccountUseCase
import net.svaroh.passly.core.accounts.usecase.accountdata.UpdateAccountDataUseCase
import net.svaroh.passly.core.accounts.usecase.privatekey.SavePrivateKeyUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import net.svaroh.passly.core.preferences.usecase.UpdateGlobalPreferencesUseCase
import net.svaroh.passly.database.usecase.SaveResourcesDatabasePassphraseUseCase
import net.svaroh.passly.intents.ManagedAccountIntentCreator
import org.koin.core.component.KoinComponent

class AccountInitializer(
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val saveResourcesDatabasePassphraseUseCase: SaveResourcesDatabasePassphraseUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val savePrivateKeyUseCase: SavePrivateKeyUseCase,
    private val managedAccountIntentCreator: ManagedAccountIntentCreator,
    private val saveAccountUseCase: SaveAccountUseCase,
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
) : KoinComponent {
    fun initializeAccount() {
        saveCurrentApiUrlUseCase.execute(
            SaveCurrentApiUrlUseCase.Input(managedAccountIntentCreator.getDomain()),
        )
        saveSelectedAccountUseCase.execute(
            UserIdInput(managedAccountIntentCreator.getUserLocalId()),
        )
        saveAccountUseCase.execute(
            UserIdInput(managedAccountIntentCreator.getUserLocalId()),
        )
        saveResourcesDatabasePassphraseUseCase.execute(
            SaveResourcesDatabasePassphraseUseCase.Input(TEST_DATABASE_PASSWORD),
        )
        updateAccountDataUseCase.execute(
            UpdateAccountDataUseCase.Input(
                userId = managedAccountIntentCreator.getUserLocalId(),
                url = managedAccountIntentCreator.getDomain(),
                firstName = managedAccountIntentCreator.getFirstName(),
                lastName = managedAccountIntentCreator.getLastName(),
                email = managedAccountIntentCreator.getUsername(),
                serverId = managedAccountIntentCreator.getUserServerId(),
            ),
        )
        savePrivateKeyUseCase.execute(
            SavePrivateKeyUseCase.Input(
                managedAccountIntentCreator.getUserLocalId(),
                managedAccountIntentCreator.getArmoredPrivateKey(),
            ),
        )
        updateGlobalPreferencesUseCase.execute(
            UpdateGlobalPreferencesUseCase.Input(
                areDebugLogsEnabled = false,
                isDeveloperModeEnabled = false,
                isHideRootDialogEnabled = false,
            ),
        )
    }

    private companion object {
        private const val TEST_DATABASE_PASSWORD = "TEST_DB_PASS"
    }
}
