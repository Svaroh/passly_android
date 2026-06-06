package net.svaroh.passly.entity.group

import androidx.room.Entity
import androidx.room.ForeignKey
import net.svaroh.passly.entity.user.User

@Entity(
    primaryKeys = ["userId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
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
data class UsersAndGroupCrossRef(
    val userId: String,
    val groupId: String,
)
