package com.passbolt.mobile.android.serializers.gson

import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.db.GetLocalMetadataKeyUseCase
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import timber.log.Timber

class MetadataEncryptor(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
    private val getLocalMetadataKeyUseCase: GetLocalMetadataKeyUseCase,
    private val openPgp: OpenPgp,
) {
    suspend fun encryptMetadata(
        metadataKeyTypeModel: MetadataKeyTypeModel,
        metadataKeyId: String,
        metadataJsonString: String,
        usersPrivateKeyPassphrase: ByteArray,
    ): Output =
        try {
            val (key, passphraseCopy) =
                when (metadataKeyTypeModel) {
                    MetadataKeyTypeModel.PERSONAL -> {
                        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
                        val privateKey = privateKeyRepository.getPrivateKey(userId)?.armoredKey
                        require(privateKey != null) { "Selected user private key not found" }
                        privateKey to usersPrivateKeyPassphrase.copyOf()
                    }
                    MetadataKeyTypeModel.SHARED -> {
                        val metadataPrivateKey =
                            getLocalMetadataKeyUseCase
                                .execute(
                                    GetLocalMetadataKeyUseCase.Input(metadataKeyId),
                                ).metadataPrivateKeys
                                .firstOrNull()

                        require(metadataPrivateKey != null) { "Metadata private key not found" }

                        metadataPrivateKey.keyData to metadataPrivateKey.passphrase.toByteArray()
                    }
                }
            try {
                val encryptedMeta =
                    openPgp.encryptSignMessageArmored(
                        key,
                        passphraseCopy,
                        metadataJsonString,
                    )

                when (encryptedMeta) {
                    is OpenPgpResult.Error -> Output.Failure(RuntimeException(encryptedMeta.error.message))
                    is OpenPgpResult.Result -> Output.Success(encryptedMeta.result)
                }
            } finally {
                passphraseCopy.erase()
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Exception during metadata encryption")
            Output.Failure(exception)
        }

    sealed class Output {
        data class Success(
            val encryptedMetadata: String,
        ) : Output()

        data class Failure(
            val error: Throwable?,
        ) : Output()
    }
}
