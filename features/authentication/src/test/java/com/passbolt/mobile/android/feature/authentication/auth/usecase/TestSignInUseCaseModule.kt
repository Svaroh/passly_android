package net.svaroh.passly.feature.authentication.auth.usecase

import net.svaroh.passly.common.CookieExtractor
import net.svaroh.passly.common.time.TimeProvider
import net.svaroh.passly.dto.request.SignInRequestDto
import net.svaroh.passly.gopenpgp.OpenPgp
import net.svaroh.passly.mappers.SignInMapper
import net.svaroh.passly.passboltapi.auth.AuthRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

internal val mockAuthRepository = mock<AuthRepository>()
internal val mockOpenPgp = mock<OpenPgp>()
internal val mockTimeProvider = mock<TimeProvider>()

val testSignInUseCaseModule =
    module {
        factory {
            mock<SignInMapper> {
                on { mapRequestToDto(any(), any()) }.doReturn(SignInRequestDto("userId", "challenge"))
            }
        }
        factory { mockAuthRepository }
        factory { mockTimeProvider }
        factory { mockOpenPgp }
        singleOf(::CookieExtractor)
        factoryOf(::SignInUseCase)
        factoryOf(::GopenPgpTimeUpdater)
    }
