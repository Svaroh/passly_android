package net.svaroh.passly.core.commonfolders.usecase.db

import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.database.ResourceDatabase
import net.svaroh.passly.database.impl.folders.FoldersDao
import net.svaroh.passly.entity.folder.Folder
import net.svaroh.passly.entity.folder.FolderUpdateState
import net.svaroh.passly.entity.resource.Permission
import net.svaroh.passly.mappers.FolderModelMapper
import net.svaroh.passly.ui.FolderModel
import net.svaroh.passly.ui.ResourcePermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class UpsertLocalFoldersUseCaseTest : KoinTest {
    private val useCase: UpsertLocalFoldersUseCase by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<FoldersDao>() }
                    single { mock<ResourceDatabase>() }
                    single { mock<DatabaseProvider>() }
                    single { mock<GetSelectedAccountUseCase>() }
                    single { mock<FolderModelMapper>() }
                    singleOf(::UpsertLocalFoldersUseCase)
                },
            )
        }

    @Before
    fun setUp() {
        whenever(get<ResourceDatabase>().foldersDao()).doReturn(get<FoldersDao>())
        get<DatabaseProvider>().stub {
            onBlocking { get(any()) }.doReturn(get<ResourceDatabase>())
        }
        whenever(get<GetSelectedAccountUseCase>().execute(Unit))
            .doReturn(GetSelectedAccountUseCase.Output(SELECTED_ACCOUNT_ID))
    }

    @Test
    fun `should map folders with updated state and call dao upsert`() =
        runTest {
            val folderModel =
                FolderModel(
                    folderId = "folderId",
                    parentFolderId = null,
                    name = "Test Folder",
                    isShared = false,
                    permission = ResourcePermission.READ,
                )
            val mappedFolder =
                Folder(
                    folderId = "folderId",
                    name = "Test Folder",
                    permission = Permission.READ,
                    parentId = null,
                    isShared = false,
                    updateState = FolderUpdateState.UPDATED,
                )
            get<FolderModelMapper>().stub {
                on { map(any<FolderModel>(), eq(FolderUpdateState.UPDATED)) }.doReturn(mappedFolder)
            }

            useCase.execute(UpsertLocalFoldersUseCase.Input(listOf(folderModel)))

            verify(get<FolderModelMapper>()).map(folderModel, folderUpdateState = FolderUpdateState.UPDATED)
            verify(get<FoldersDao>()).upsertAll(listOf(mappedFolder))
        }

    private companion object {
        private const val SELECTED_ACCOUNT_ID = "selectedAccountId"
    }
}
