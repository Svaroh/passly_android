package net.svaroh.passly.mappers

import net.svaroh.passly.dto.response.UserDto
import net.svaroh.passly.entity.user.User
import net.svaroh.passly.entity.user.UserGpgKey
import net.svaroh.passly.entity.user.UserProfile
import net.svaroh.passly.ui.GpgKeyModel
import net.svaroh.passly.ui.UserModel
import net.svaroh.passly.ui.UserProfileModel
import net.svaroh.passly.ui.UserWithAvatar
import java.time.ZonedDateTime

class UsersModelMapper {
    fun map(userDto: UserDto): UserModel {
        val usersGpgKey = requireNotNull(userDto.gpgKey)
        return UserModel(
            id = userDto.id.toString(),
            userName = userDto.username,
            // if disabled date is in the past the user is disabled
            disabled =
                userDto.disabled
                    ?.let { ZonedDateTime.parse(it).isBefore(ZonedDateTime.now()) } ?: false,
            gpgKey =
                GpgKeyModel(
                    id = usersGpgKey.id.toString(),
                    armoredKey = usersGpgKey.armoredKey,
                    fingerprint = usersGpgKey.fingerprint,
                    bits = usersGpgKey.bits,
                    uid = usersGpgKey.uid,
                    keyId = usersGpgKey.keyId,
                    type = usersGpgKey.type,
                    keyExpirationDate = usersGpgKey.expires?.let { expires -> ZonedDateTime.parse(expires) },
                    keyCreationDate = usersGpgKey.keyCreated?.let { keyCreated -> ZonedDateTime.parse(keyCreated) },
                ),
            profile =
                UserProfileModel(
                    username = userDto.username,
                    firstName = userDto.profile?.firstName,
                    lastName = userDto.profile?.lastName,
                    avatarUrl =
                        userDto.profile
                            ?.avatar
                            ?.url
                            ?.medium,
                ),
        )
    }

    fun map(input: List<UserDto>): List<UserModel> =
        input
            .filter { it.active && !it.deleted }
            .map(::map)

    fun map(input: UserModel) =
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
        )

    fun map(input: User) =
        UserModel(
            id = input.id,
            userName = input.userName,
            gpgKey =
                GpgKeyModel(
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
                UserProfileModel(
                    username = input.userName,
                    firstName = input.profile.firstName,
                    lastName = input.profile.lastName,
                    avatarUrl = input.profile.avatarUrl,
                ),
        )

    fun mapToUserWithAvatar(input: UserModel) =
        UserWithAvatar(
            userId = input.id,
            firstName = input.profile.firstName.orEmpty(),
            lastName = input.profile.lastName.orEmpty(),
            userName = input.userName,
            avatarUrl = input.profile.avatarUrl,
            isDisabled = input.disabled,
        )
}
