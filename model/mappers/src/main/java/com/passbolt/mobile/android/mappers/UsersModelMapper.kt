package net.svaroh.passly.mappers

import net.svaroh.passly.entity.user.User
import net.svaroh.passly.entity.user.UserGpgKey
import net.svaroh.passly.entity.user.UserProfile
import net.svaroh.passly.entity.user.UserUpdateState
import net.svaroh.passly.ui.GpgKeyUiModel
import net.svaroh.passly.ui.UserProfileUiModel
import net.svaroh.passly.ui.UserUiModel
import net.svaroh.passly.ui.UserWithAvatar

class UsersModelMapper {
    fun map(input: UserUiModel) =
        User(
            id = input.id,
            userName = input.userName,
            profile =
                UserProfile(
                    firstName = input.profile.firstName,
                    lastName = input.profile.lastName,
                    avatarUrl = input.profile.avatarUrl,
                ),
            disabled = input.disabled,
            gpgKey =
                UserGpgKey(
                    id = input.gpgKey.id,
                    armoredKey = input.gpgKey.armoredKey,
                    bits = input.gpgKey.bits,
                    uid = input.gpgKey.uid,
                    keyId = input.gpgKey.keyId,
                    fingerprint = input.gpgKey.fingerprint,
                    type = input.gpgKey.type,
                    expires = input.gpgKey.keyExpirationDate,
                    created = input.gpgKey.keyCreationDate,
                ),
            updateState = UserUpdateState.UPDATED,
        )

    fun map(input: User) =
        UserUiModel(
            id = input.id,
            userName = input.userName,
            gpgKey =
                GpgKeyUiModel(
                    id = input.gpgKey.id,
                    armoredKey = input.gpgKey.armoredKey,
                    fingerprint = input.gpgKey.fingerprint,
                    bits = input.gpgKey.bits,
                    uid = input.gpgKey.uid,
                    keyId = input.gpgKey.keyId,
                    type = input.gpgKey.type,
                    keyExpirationDate = input.gpgKey.expires,
                    keyCreationDate = input.gpgKey.created,
                ),
            disabled = input.disabled,
            profile =
                UserProfileUiModel(
                    username = input.userName,
                    firstName = input.profile.firstName,
                    lastName = input.profile.lastName,
                    avatarUrl = input.profile.avatarUrl,
                ),
        )

    fun mapToUserWithAvatar(input: UserUiModel) =
        UserWithAvatar(
            userId = input.id,
            firstName = input.profile.firstName.orEmpty(),
            lastName = input.profile.lastName.orEmpty(),
            userName = input.userName,
            avatarUrl = input.profile.avatarUrl,
            isDisabled = input.disabled,
        )
}
