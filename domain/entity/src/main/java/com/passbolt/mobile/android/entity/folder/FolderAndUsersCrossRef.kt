package net.svaroh.passly.entity.folder

import androidx.room.Entity
import androidx.room.ForeignKey
import net.svaroh.passly.entity.resource.Permission
import net.svaroh.passly.entity.user.User

@Entity(
    primaryKeys = ["userId", "folderId"],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["folderId"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FolderAndUsersCrossRef(
    val folderId: String,
    val userId: String,
    val permission: Permission,
    val permissionId: String,
)
