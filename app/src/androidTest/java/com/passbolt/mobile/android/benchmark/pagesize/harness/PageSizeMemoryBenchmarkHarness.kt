/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package net.svaroh.passly.benchmark.pagesize.harness

import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import net.svaroh.passly.benchmark.pagesize.appstate.DecryptionPassphrase
import net.svaroh.passly.benchmark.pagesize.appstate.LocalResourceStore
import net.svaroh.passly.benchmark.pagesize.appstate.ResourceTypeSeeder
import net.svaroh.passly.benchmark.pagesize.appstate.SessionKeyCacheSeeder
import net.svaroh.passly.benchmark.pagesize.fixture.MetadataDocumentFactory
import net.svaroh.passly.benchmark.pagesize.fixture.MetadataProfile
import net.svaroh.passly.benchmark.pagesize.fixture.PeskEncryptor
import net.svaroh.passly.benchmark.pagesize.fixture.ResourceCorpus
import net.svaroh.passly.benchmark.pagesize.fixture.ResourcePagePayloadFactory
import net.svaroh.passly.benchmark.pagesize.server.LoopbackMockServer
import net.svaroh.passly.benchmark.pagesize.server.MockApiRedirect
import net.svaroh.passly.benchmark.pagesize.server.ResourcePageDispatcher
import net.svaroh.passly.benchmark.pagesize.sweep.BenchmarkDeviceFingerprint
import net.svaroh.passly.benchmark.pagesize.sweep.BenchmarkResultRecorder
import net.svaroh.passly.benchmark.pagesize.sweep.CsvBenchmarkResultRecorder
import net.svaroh.passly.benchmark.pagesize.sweep.HeapSampler
import net.svaroh.passly.benchmark.pagesize.sweep.MeasuredResourceRefresh
import net.svaroh.passly.benchmark.pagesize.sweep.OomSurvivingUncaughtHandler
import net.svaroh.passly.benchmark.pagesize.sweep.PageSizeSweep
import net.svaroh.passly.benchmark.pagesize.sweep.PipelineWarmUp
import net.svaroh.passly.benchmark.pagesize.sweep.RowWatchdog
import net.svaroh.passly.benchmark.pagesize.sweep.RunBudget
import net.svaroh.passly.benchmark.pagesize.sweep.logBenchmark
import net.svaroh.passly.feature.authentication.AuthenticationMainActivity
import net.svaroh.passly.gopenpgp.OpenPgp
import net.svaroh.passly.gopenpgp.exception.OpenPgpResult
import net.svaroh.passly.instrumentationTestsModule
import net.svaroh.passly.intents.ManagedAccountIntentCreator
import net.svaroh.passly.rules.LazyKoinAuthenticationActivityScenarioRule
import net.svaroh.passly.rules.lazyActivitySetupScenarioRule
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File
import java.util.UUID

class PageSizeMemoryBenchmarkHarness(
    private val metadataProfile: MetadataProfile,
    private val seedSessionKeys: Boolean = true,
) : KoinComponent {
    private val context: Context get() = getInstrumentation().targetContext

    fun execute() {
        val runBudget = RunBudget(RUN_BUDGET_MS)
        val account = get<ManagedAccountIntentCreator>()
        val userId = account.getUserLocalId()
        val payloadFactory = createPayloadFactory(account, userId)
        val corpus = ResourceCorpus()

        LoopbackMockServer(ResourcePageDispatcher(corpus, payloadFactory)).use { server ->
            MockApiRedirect(get(), get()).redirectTo(server.url)
            ResourceTypeSeeder(get(), get()).seedV5Default(userId = userId, resourceTypeId = RESOURCE_TYPE_ID)

            val localResourceStore = LocalResourceStore(get(), get(), get(), userId)

            OomSurvivingUncaughtHandler().use { oomHandler ->
                val refresh = createMeasuredRefresh(account, corpus, localResourceStore, payloadFactory, oomHandler)

                if (seedSessionKeys) verifySymmetricDecryptContract(payloadFactory)

                PipelineWarmUp(refresh, localResourceStore)
                    .verifyDecryptsAndPersists(WARM_UP_SIZE) { "mockRequestCount=${server.handledRequestCount}." }

                val recorder = createRecorder(payloadFactory)
                PageSizeSweep(refresh, localResourceStore, recorder, oomHandler, runBudget)
                    .run(PAGE_SIZE_SWEEP, TOTAL_RESOURCES_PER_RUN)
                logBenchmark("run finished in ${runBudget.elapsedMs() / MS_IN_S}s of a ${RUN_BUDGET_MS / MS_IN_S}s budget")
                println(recorder.snapshot())
            }
        }
    }

    private fun createPayloadFactory(
        account: ManagedAccountIntentCreator,
        userId: String,
    ): ResourcePagePayloadFactory {
        val gson = get<Gson>()
        return ResourcePagePayloadFactory(
            gson = gson,
            resourceTypeId = RESOURCE_TYPE_ID,
            ownerUserId = UUID.fromString(userId),
            documentFactory = MetadataDocumentFactory(gson, metadataProfile),
            encryptor = PeskEncryptor(account.getArmoredPrivateKey(), account.getPassphrase().toByteArray()),
        )
    }

    private fun createMeasuredRefresh(
        account: ManagedAccountIntentCreator,
        corpus: ResourceCorpus,
        localResourceStore: LocalResourceStore,
        payloadFactory: ResourcePagePayloadFactory,
        oomHandler: OomSurvivingUncaughtHandler,
    ): MeasuredResourceRefresh {
        val seeder = SessionKeyCacheSeeder(get())
        val seed: (List<UUID>) -> Unit =
            if (seedSessionKeys) {
                { ids -> seeder.seed(ids, payloadFactory::sessionKeyHexForCorpusIndex) }
            } else {
                { }
            }
        return MeasuredResourceRefresh(
            resourceInteractor = get(),
            updateGlobalPreferencesUseCase = get(),
            corpus = corpus,
            localResourceStore = localResourceStore,
            decryptionPassphrase = DecryptionPassphrase(get()) { account.getPassphrase().toByteArray() },
            heapSampler = HeapSampler(),
            seedSessionKeys = seed,
            oomHandler = oomHandler,
            watchdog = RowWatchdog(),
        )
    }

    private fun verifySymmetricDecryptContract(payloadFactory: ResourcePagePayloadFactory) {
        val probe = payloadFactory.probe
        val result =
            runBlocking {
                get<OpenPgp>().decryptMessageArmoredWithSessionKey(probe.sessionKeyHex, probe.armoredMessage)
            }
        check(result is OpenPgpResult.Result) {
            "Seeded session key does not symmetrically decrypt the fixture metadata ($result); " +
                "the warm-path benchmark would fall back to per-resource asymmetric decrypt. Check session key hex/algo."
        }
    }

    private fun createRecorder(payloadFactory: ResourcePagePayloadFactory): BenchmarkResultRecorder {
        val fingerprint = BenchmarkDeviceFingerprint.capture(context)
        return CsvBenchmarkResultRecorder(
            file = csvFile(fingerprint),
            fingerprint = fingerprint,
            profile = metadataProfile,
            metadataPlaintextBytes = payloadFactory.approximateMetadataPlaintextBytes,
            poolSize = payloadFactory.poolSize,
            totalResourcesPerRun = TOTAL_RESOURCES_PER_RUN,
            sessionKeyMode = if (seedSessionKeys) WARM_SESSION_KEYS else COLD_SESSION_KEYS,
            runBudgetMs = RUN_BUDGET_MS,
        )
    }

    private fun csvFile(fingerprint: BenchmarkDeviceFingerprint): File =
        File(
            context.getExternalFilesDir(null),
            "pagesize-benchmark-${fingerprint.model.replace(' ', '_')}-${metadataProfile.name}.csv",
        )

    private companion object {
        private val RESOURCE_TYPE_ID: UUID = UUID.fromString("a0b1c2d3-0000-4000-8000-000000000005")
        private const val WARM_UP_SIZE = 25
        private const val TOTAL_RESOURCES_PER_RUN = 10_000
        private val PAGE_SIZE_SWEEP = listOf(250, 500, 1_000, 2_000, 5_000, 10_000)
        private const val RUN_BUDGET_MS = 30 * 60 * 1_000L
        private const val MS_IN_S = 1_000L
        private const val WARM_SESSION_KEYS = "warm"
        private const val COLD_SESSION_KEYS = "cold"
    }
}

internal fun benchmarkSetupRule(): LazyKoinAuthenticationActivityScenarioRule<AuthenticationMainActivity> =
    lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
        launchActivity = false,
        koinOverrideModules = listOf(instrumentationTestsModule),
        intentSupplier = { Intent() },
    )
