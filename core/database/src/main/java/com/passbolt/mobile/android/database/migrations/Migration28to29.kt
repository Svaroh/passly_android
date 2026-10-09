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
package net.svaroh.passly.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the local secret store. Purely additive and network independent, so an app update applied while offline
 * cannot leave an account unusable.
 */
@Suppress("MagicNumber")
object Migration28to29 : Migration(28, 29) {
    private const val CREATE_SECRET_TABLE =
        "CREATE TABLE IF NOT EXISTS `Secret` (" +
            "`resourceId` TEXT NOT NULL, " +
            "`secretId` TEXT, " +
            "`armoredData` TEXT NOT NULL, " +
            "`modified` INTEGER, " +
            "`fetchedAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`resourceId`), " +
            "FOREIGN KEY(`resourceId`) REFERENCES `Resource`(`resourceId`) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE )"

    private val CREATE_INDICES =
        listOf(
            "CREATE INDEX IF NOT EXISTS `index_Resource_folderId` ON `Resource` (`folderId`)",
            "CREATE INDEX IF NOT EXISTS `index_Resource_resourceTypeId` ON `Resource` (`resourceTypeId`)",
            "CREATE INDEX IF NOT EXISTS `index_Resource_modified_resourceId` ON `Resource` (`modified` DESC, `resourceId` ASC)",
            "CREATE INDEX IF NOT EXISTS `index_Resource_expiry_resourceId` ON `Resource` (`expiry`, `resourceId`)",
            "CREATE INDEX IF NOT EXISTS `index_Resource_favouriteId` ON `Resource` (`favouriteId`)",
            "CREATE INDEX IF NOT EXISTS `index_Folder_parentId` ON `Folder` (`parentId`)",
            "CREATE INDEX IF NOT EXISTS `index_Folder_modified_folderId` ON `Folder` (`modified` DESC, `folderId` ASC)",
            "CREATE INDEX IF NOT EXISTS `index_ResourceAndTagsCrossRef_resourceId` ON `ResourceAndTagsCrossRef` (`resourceId`)",
            "CREATE INDEX IF NOT EXISTS `index_ResourceAndGroupsCrossRef_groupId` ON `ResourceAndGroupsCrossRef` (`groupId`)",
            "CREATE INDEX IF NOT EXISTS `index_UsersAndGroupCrossRef_groupId` ON `UsersAndGroupCrossRef` (`groupId`)",
            "CREATE INDEX IF NOT EXISTS `index_ResourceAndUsersCrossRef_userId` ON `ResourceAndUsersCrossRef` (`userId`)",
            "CREATE INDEX IF NOT EXISTS `index_FolderAndUsersCrossRef_folderId` ON `FolderAndUsersCrossRef` (`folderId`)",
            "CREATE INDEX IF NOT EXISTS `index_FolderAndGroupsCrossRef_groupId` ON `FolderAndGroupsCrossRef` (`groupId`)",
            "CREATE INDEX IF NOT EXISTS `index_ResourceUri_resourceId` ON `ResourceUri` (`resourceId`)",
            "CREATE INDEX IF NOT EXISTS `index_MetadataPrivateKey_metadataKeyId` ON `MetadataPrivateKey` (`metadataKeyId`)",
        )

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_SECRET_TABLE)
        CREATE_INDICES.forEach { sql ->
            db.execSQL(sql)
        }
    }
}
