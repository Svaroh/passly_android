/**
 * Passly - Open source password manager for teams
 * Copyright (c) 2026 Svaroh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Svaroh
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://passly.svaroh.net Passly
 * @since v1.0
 */
package net.svaroh.passly.feature.authentication.auth.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.svaroh.passly.common.extension.erase
import net.svaroh.passly.core.accounts.usecase.accountdata.SaveServerFingerprintUseCase
import net.svaroh.passly.core.authenticationcore.session.SaveSessionUseCase
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import timber.log.Timber

/**
 * Establishes a server session after the user has already been let in with the local replica.
 *
 * Unlocking is a local decision: the passphrase is verified against the locally stored private key, so waiting for
 * the server before showing the vault only ever adds delay - minutes of it when the domain resolves but the site does
 * not answer. The session is still worth having, because synchronisation needs it, so it is obtained here, off the
 * critical path, in a scope that outlives the sign in screen.
 *
 * Every failure is silent by design. There is nothing for the user to do about an unreachable server at this point,
 * and the app is fully usable without the session; the next synchronisation will try again.
 */
class BackgroundSignInExecutor(
    private val getAndVerifyServerKeysInteractor: GetAndVerifyServerKeysAndTimeInteractor,
    private val signInVerifyInteractor: SignInVerifyInteractor,
    private val saveSessionUseCase: SaveSessionUseCase,
    private val saveServerFingerprintUseCase: SaveServerFingerprintUseCase,
    coroutineLaunchContext: CoroutineLaunchContext,
) {
    private val scope = CoroutineScope(SupervisorJob() + coroutineLaunchContext.io)

    private var runningJob: Job? = null

    /**
     * @param passphrase a copy owned by this executor - it is erased once the attempt finishes.
     */
    fun signIn(
        userId: String,
        passphrase: ByteArray,
    ) {
        if (runningJob?.isActive == true) {
            Timber.d("[BackgroundSignIn] Already running, skipping")
            passphrase.erase()
            return
        }
        runningJob =
            scope.launch {
                try {
                    obtainSession(userId, passphrase)
                } catch (exception: Exception) {
                    Timber.d(exception, "[BackgroundSignIn] Could not obtain a session")
                } finally {
                    passphrase.erase()
                }
            }
    }

    private suspend fun obtainSession(
        userId: String,
        passphrase: ByteArray,
    ) {
        getAndVerifyServerKeysInteractor.getAndVerifyServerKeys(
            userId,
            onError = { Timber.d("[BackgroundSignIn] Server keys unavailable: $it") },
        ) { serverKeys ->
            signInVerifyInteractor.signInVerify(
                serverKeys.pgpKey,
                passphrase,
                userId,
                serverKeys.rsaKey,
                onError = { Timber.d("[BackgroundSignIn] Sign in did not succeed: $it") },
            ) { signInResult ->
                Timber.d("[BackgroundSignIn] Session established")
                saveSessionUseCase.execute(
                    SaveSessionUseCase.Input(
                        userId = userId,
                        accessToken = signInResult.accessToken,
                        refreshToken = signInResult.refreshToken,
                        mfaToken = signInResult.mfaToken,
                    ),
                )
                saveServerFingerprintUseCase.execute(
                    SaveServerFingerprintUseCase.Input(userId, serverKeys.pgpKeyFingerprint),
                )
            }
        }
    }
}
