package com.passbolt.mobile.android.serializers.gson

import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.request.CreateV4ResourceDto
import com.passbolt.mobile.android.dto.request.CreateV5ResourceDto
import com.passbolt.mobile.android.dto.request.EncryptedSecret
import java.lang.reflect.Type

class CreateResourceModelSerializer : JsonSerializer<CreateResourceDto> {
    override fun serialize(
        src: CreateResourceDto,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement =
        when (src) {
            is CreateV4ResourceDto -> {
                context
                    .serialize(src, CreateV4ResourceDto::class.java)
                    .withoutNullSecrets(src.secrets)
            }
            is CreateV5ResourceDto -> {
                context
                    .serialize(src, CreateV5ResourceDto::class.java)
                    .withoutNullSecrets(src.secrets)
            }
        }

    // add for case of usage without passing secrets (resource with only metadata changes)
    // in that case don't send secrets = null (backend ignores it, but it's more future-proof to remove it totally)
    private fun JsonElement.withoutNullSecrets(secrets: List<EncryptedSecret>?): JsonElement =
        apply {
            if (secrets == null) {
                asJsonObject.remove(SECRETS_KEY)
            }
        }

    private companion object {
        private const val SECRETS_KEY = "secrets"
    }
}
