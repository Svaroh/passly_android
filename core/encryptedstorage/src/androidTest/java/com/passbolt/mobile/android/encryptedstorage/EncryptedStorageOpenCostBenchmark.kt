package net.svaroh.passly.encryptedstorage

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureNanoTime

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

@RunWith(AndroidJUnit4::class)
class EncryptedStorageOpenCostBenchmark {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val masterKey =
        MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    @Before
    fun setup() {
        openPreferences().edit().putString(KEY, VALUE).commit()
        File(context.filesDir, FILE_NAME).delete()
        buildFile().openFileOutput().use { it.write(VALUE.toByteArray()) }
    }

    @Test
    fun measureOpenCosts() {
        val uncachedRead = measure { openPreferences().getString(KEY, null) }
        val cachedPreferences = openPreferences()
        val cachedRead = measure { cachedPreferences.getString(KEY, null) }
        val fileBuild = measure { buildFile() }
        val cachedFile = buildFile()
        val cachedFileRead = measure { cachedFile.openFileInput().use { it.readBytes() } }

        Log.i(TAG, "${Build.MANUFACTURER} ${Build.MODEL}, API ${Build.VERSION.SDK_INT}")
        Log.i(TAG, "EncryptedSharedPreferences.create + getString: ${uncachedRead.summary()}")
        Log.i(TAG, "cached getString: ${cachedRead.summary()}")
        Log.i(TAG, "EncryptedFile.Builder.build: ${fileBuild.summary()}")
        Log.i(TAG, "cached EncryptedFile read: ${cachedFileRead.summary()}")
    }

    private fun openPreferences() = EncryptedSharedPreferences.create(context, PREFERENCES_NAME, masterKey, AES256_SIV, AES256_GCM)

    private fun buildFile() =
        EncryptedFile
            .Builder(
                context,
                File(context.filesDir, FILE_NAME),
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
            ).build()

    private fun measure(block: () -> Unit): List<Long> = List(RUNS) { measureNanoTime(block) }.sorted()

    private fun List<Long>.summary(): String {
        fun ms(index: Int) = "%.1f".format(this[index] / NANOS_IN_MILLI)
        return "median ${ms(size / 2)} ms, p90 ${ms(size * 9 / 10)} ms, max ${ms(size - 1)} ms (n=$size)"
    }

    private companion object {
        private const val TAG = "EncryptedStorageOpenCostBenchmark"
        private const val PREFERENCES_NAME = "open_cost_benchmark.xml"
        private const val FILE_NAME = "open_cost_benchmark_file"
        private const val KEY = "key"
        private const val VALUE = "value"
        private const val RUNS = 50
        private const val NANOS_IN_MILLI = 1_000_000.0
    }
}
