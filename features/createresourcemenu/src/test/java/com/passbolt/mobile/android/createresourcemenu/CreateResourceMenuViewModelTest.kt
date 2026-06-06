package net.svaroh.passly.createresourcemenu

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.Close
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.CreateFolder
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.CreateNote
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.CreatePassword
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.CreateTotp
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.Initialize
import net.svaroh.passly.createresourcemenu.CreateResourceMenuSideEffect.InvokeCreateFolder
import net.svaroh.passly.createresourcemenu.CreateResourceMenuSideEffect.InvokeCreateNote
import net.svaroh.passly.createresourcemenu.CreateResourceMenuSideEffect.InvokeCreatePassword
import net.svaroh.passly.createresourcemenu.CreateResourceMenuSideEffect.InvokeCreateTotp
import net.svaroh.passly.createresourcemenu.usecase.CreateCreateResourceMenuModelUseCase
import net.svaroh.passly.ui.CreateResourceMenuModel
import net.svaroh.passly.ui.HomeDisplayViewModel.AllItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class CreateResourceMenuViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<CreateCreateResourceMenuModelUseCase>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::CreateResourceMenuViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: CreateResourceMenuViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val createCreateResourceMenuModelUseCase: CreateCreateResourceMenuModelUseCase = get()
        createCreateResourceMenuModelUseCase.stub {
            onBlocking { execute(any()) } doReturn
                CreateCreateResourceMenuModelUseCase.Output(
                    model =
                        CreateResourceMenuModel(
                            isPasswordEnabled = true,
                            isTotpEnabled = true,
                            isFolderEnabled = true,
                            isNoteEnabled = true,
                        ),
                )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize should update view state with model data`() =
        runTest {
            viewModel = get()
            val homeDisplayViewModel = AllItems

            viewModel.onIntent(Initialize(homeDisplayViewModel))

            viewModel.viewState.drop(1).test {
                val state = awaitItem()
                assertThat(state.showPasswordButton).isTrue()
                assertThat(state.showTotpButton).isTrue()
                assertThat(state.showFoldersButton).isTrue()
                assertThat(state.showNoteButton).isTrue()
            }
        }

    @Test
    fun `items should be hidden based on model`() =
        runTest {
            val createCreateResourceMenuModelUseCase: CreateCreateResourceMenuModelUseCase = get()
            createCreateResourceMenuModelUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    CreateCreateResourceMenuModelUseCase.Output(
                        model =
                            CreateResourceMenuModel(
                                isPasswordEnabled = false,
                                isTotpEnabled = false,
                                isFolderEnabled = false,
                                isNoteEnabled = false,
                            ),
                    )
            }

            viewModel = get()
            val homeDisplayViewModel = AllItems

            viewModel.onIntent(Initialize(homeDisplayViewModel))

            viewModel.viewState.test {
                val state = awaitItem()
                Truth.assertThat(state.showPasswordButton).isFalse()
                Truth.assertThat(state.showTotpButton).isFalse()
                Truth.assertThat(state.showFoldersButton).isFalse()
                Truth.assertThat(state.showNoteButton).isFalse()
            }
        }

    @Test
    fun `note item should be visible when enabled`() =
        runTest {
            val createCreateResourceMenuModelUseCase: CreateCreateResourceMenuModelUseCase = get()
            createCreateResourceMenuModelUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    CreateCreateResourceMenuModelUseCase.Output(
                        model =
                            CreateResourceMenuModel(
                                isPasswordEnabled = false,
                                isTotpEnabled = false,
                                isFolderEnabled = false,
                                isNoteEnabled = true,
                            ),
                    )
            }

            viewModel = get()
            val homeDisplayViewModel = AllItems

            viewModel.onIntent(Initialize(homeDisplayViewModel))

            viewModel.viewState.drop(1).test {
                val state = awaitItem()
                assertThat(state.showPasswordButton).isFalse()
                assertThat(state.showTotpButton).isFalse()
                assertThat(state.showFoldersButton).isFalse()
                assertThat(state.showNoteButton).isTrue()
            }
        }

    @Test
    fun `intent close should emit dismiss side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Close)
                val effect = awaitItem()
                assertThat(effect).isEqualTo(CreateResourceMenuSideEffect.Dismiss)
            }
        }

    @Test
    fun `intent create password should emit create password side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreatePassword)
                val effect = awaitItem()
                assertThat(effect).isEqualTo(InvokeCreatePassword)
            }
        }

    @Test
    fun `intent create totp should emit create totp side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateTotp)
                val effect = awaitItem()
                assertThat(effect).isEqualTo(InvokeCreateTotp)
            }
        }

    @Test
    fun `intent create folder should emit create folder side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateFolder)
                val effect = awaitItem()
                assertThat(effect).isEqualTo(InvokeCreateFolder)
            }
        }

    @Test
    fun `intent create note should emit create note side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateNote)
                val effect = awaitItem()
                assertThat(effect).isEqualTo(InvokeCreateNote)
            }
        }
}
