package net.svaroh.passly.entity.group

import androidx.room.Entity
import androidx.room.ForeignKey
import net.svaroh.passly.entity.folder.Folder
import net.svaroh.passly.entity.resource.Permission

@Entity(
    primaryKeys = ["folderId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["folderId"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UsersGroup::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FolderAndGroupsCrossRef(
    val folderId: String,
    val groupId: String,
    val permission: Permission,
    val permissionId: String,
)
