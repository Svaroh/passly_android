package net.svaroh.passly.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.svaroh.passly.database.impl.folderandgroupscrossref.FolderAndGroupsCrossRefDao
import net.svaroh.passly.database.impl.folderanduserscrossref.FoldersAndUsersCrossRefDao
import net.svaroh.passly.database.impl.folders.FoldersDao
import net.svaroh.passly.database.impl.folders.PaginatedFoldersDao
import net.svaroh.passly.database.impl.groups.GroupsDao
import net.svaroh.passly.database.impl.groups.PaginatedGroupsDao
import net.svaroh.passly.database.impl.metadata.MetadataKeysDao
import net.svaroh.passly.database.impl.metadata.MetadataPrivateKeysDao
import net.svaroh.passly.database.impl.metadata.ResourceMetadataDao
import net.svaroh.passly.database.impl.metadata.ResourceUriDao
import net.svaroh.passly.database.impl.resourceandgroupscrossref.ResourceAndGroupsCrossRefDao
import net.svaroh.passly.database.impl.resourceandtagcrossref.ResourcesAndTagsCrossRefDao
import net.svaroh.passly.database.impl.resourceanduserscrossref.ResourcesAndUsersCrossRefDao
import net.svaroh.passly.database.impl.resources.PaginatedResourcesDao
import net.svaroh.passly.database.impl.resources.ResourcesDao
import net.svaroh.passly.database.impl.resourcetypes.ResourceTypesDao
import net.svaroh.passly.database.impl.tags.PaginatedTagsDao
import net.svaroh.passly.database.impl.tags.TagsDao
import net.svaroh.passly.database.impl.users.UsersDao
import net.svaroh.passly.database.impl.usersandgroupscrossref.UsersAndGroupsCrossRefDao
import net.svaroh.passly.database.typeconverters.Converters
import net.svaroh.passly.entity.folder.Folder
import net.svaroh.passly.entity.folder.FolderAndUsersCrossRef
import net.svaroh.passly.entity.folder.FolderFts
import net.svaroh.passly.entity.group.FolderAndGroupsCrossRef
import net.svaroh.passly.entity.group.ResourceAndGroupsCrossRef
import net.svaroh.passly.entity.group.UsersAndGroupCrossRef
import net.svaroh.passly.entity.group.UsersGroup
import net.svaroh.passly.entity.group.UsersGroupFts
import net.svaroh.passly.entity.metadata.MetadataKey
import net.svaroh.passly.entity.metadata.MetadataPrivateKey
import net.svaroh.passly.entity.resource.Resource
import net.svaroh.passly.entity.resource.ResourceAndTagsCrossRef
import net.svaroh.passly.entity.resource.ResourceMetadata
import net.svaroh.passly.entity.resource.ResourceMetadataFts
import net.svaroh.passly.entity.resource.ResourceType
import net.svaroh.passly.entity.resource.ResourceUri
import net.svaroh.passly.entity.resource.ResourceUriFts
import net.svaroh.passly.entity.resource.Tag
import net.svaroh.passly.entity.resource.TagFts
import net.svaroh.passly.entity.user.ResourceAndUsersCrossRef
import net.svaroh.passly.entity.user.User

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

@Database(
    entities = [
        Resource::class,
        Folder::class,
        ResourceType::class,
        Tag::class,
        ResourceAndTagsCrossRef::class,
        UsersGroup::class,
        ResourceAndGroupsCrossRef::class,
        User::class,
        UsersAndGroupCrossRef::class,
        ResourceAndUsersCrossRef::class,
        FolderAndUsersCrossRef::class,
        FolderAndGroupsCrossRef::class,
        ResourceMetadata::class,
        ResourceMetadataFts::class,
        ResourceUri::class,
        ResourceUriFts::class,
        TagFts::class,
        FolderFts::class,
        UsersGroupFts::class,
        MetadataKey::class,
        MetadataPrivateKey::class,
    ],
    version = 23,
)
@TypeConverters(Converters::class)
abstract class ResourceDatabase : RoomDatabase() {
    abstract fun resourcesDao(): ResourcesDao

    abstract fun paginatedResourcesDao(): PaginatedResourcesDao

    abstract fun resourceMetadataDao(): ResourceMetadataDao

    abstract fun resourceUriDao(): ResourceUriDao

    abstract fun resourceTypesDao(): ResourceTypesDao

    abstract fun foldersDao(): FoldersDao

    abstract fun paginatedFoldersDao(): PaginatedFoldersDao

    abstract fun tagsDao(): TagsDao

    abstract fun paginatedTagsDao(): PaginatedTagsDao

    abstract fun resourcesAndTagsCrossRefDao(): ResourcesAndTagsCrossRefDao

    abstract fun groupsDao(): GroupsDao

    abstract fun paginatedGroupsDao(): PaginatedGroupsDao

    abstract fun resourcesAndGroupsCrossRefDao(): ResourceAndGroupsCrossRefDao

    abstract fun usersDao(): UsersDao

    abstract fun usersAndGroupsCrossRefDao(): UsersAndGroupsCrossRefDao

    abstract fun resourcesAndUsersCrossRefDao(): ResourcesAndUsersCrossRefDao

    abstract fun folderAndGroupsCrossRefDao(): FolderAndGroupsCrossRefDao

    abstract fun folderAndUsersCrossRefDao(): FoldersAndUsersCrossRefDao

    abstract fun metadataKeysDao(): MetadataKeysDao

    abstract fun metadataPrivateKeysDao(): MetadataPrivateKeysDao
}
