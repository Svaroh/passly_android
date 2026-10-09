package net.svaroh.passly.accountinit

import android.os.Build
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.domain.accounts.usecase.SaveAccountUseCase
import net.svaroh.passly.domain.accounts.usecase.SaveCurrentApiUrlUseCase
import net.svaroh.passly.domain.accounts.usecase.SaveSelectedAccountUseCase
import net.svaroh.passly.domain.accounts.usecase.UpdateAccountDataUseCase
import net.svaroh.passly.domain.auth.usecase.SaveResourcesDatabasePassphraseUseCase
import net.svaroh.passly.domain.preferences.GlobalPreferencesRepository
import net.svaroh.passly.domain.preferences.GlobalPreferencesUpdate
import net.svaroh.passly.domain.privatekey.PrivateKeyRepository
import net.svaroh.passly.domain.privatekey.model.PrivateKey
import net.svaroh.passly.intents.ManagedAccountIntentCreator
import org.koin.core.component.KoinComponent

class AccountInitializer(
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val saveResourcesDatabasePassphraseUseCase: SaveResourcesDatabasePassphraseUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
    private val managedAccountIntentCreator: ManagedAccountIntentCreator,
    private val saveAccountUseCase: SaveAccountUseCase,
    private val globalPreferencesRepository: GlobalPreferencesRepository,
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
        privateKeyRepository.savePrivateKey(
            managedAccountIntentCreator.getUserLocalId(),
            PrivateKey(managedAccountIntentCreator.getArmoredPrivateKey()),
        )
        globalPreferencesRepository.updateGlobalPreferences(
            GlobalPreferencesUpdate(
                areDebugLogsEnabled = false,
                isHideRootDialogEnabled = false,
                deprecatedOsWarningHiddenForSdk = Build.VERSION.SDK_INT,
            ),
        )
    }

    private companion object {
        private const val TEST_DATABASE_PASSWORD = "TEST_DB_PASS"
    }
}
