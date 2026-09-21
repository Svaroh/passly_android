package com.passbolt.mobile.android.feature.authentication.auth.challenge

import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.feature.base.readFromFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ChallengeVerifierTest : KoinTest {
    private val challengeVerifier: ChallengeVerifier by inject()
    private val public = readFromFile("/server_rsa_key")
    private val wrongPublic = readFromFile("/wrong_server_rsa_key")
    private val accessToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wvcGFzc2JvbHQuZGV2XC8iLCJzdWIiOiJlMWViYzU5Mi1iO" +
            "TBkLTVlMjItOWY0MC01MGU1MjkxMTY3M2IiLCJleHAiOjE2MjQ0NTIxMDd9.IYI-hT-w9oCLfEon_ZBkX91vXTnmGYO1TtIWKHzrc" +
            "qCg4NTvDblNx-YiTGQGKN3uZVc8OTiaYveholGClfTYKhZPAPxBR6bIV275bpEXQBsjjhu_U_KrUjI84t3tv2feGCAiaicijdjWy8I8" +
            "mPM5tGKPC7P_DFo8_E_9Oibp-Ex8jvO5MlKJy-Iv9LpbnE0WqviMjQ9GVmRYzjuHuayg_JnP-7vSFZvJMOA_YlbN9L7xwOTVygZqJE3u8vE" +
            "-WoYa_G3_aYk5gJYw50LAPZ7PXF8kjv7J01UtSVCZTJ7FmE3AvncN8mG6XgEG1501fUGeQpkVnFW7o99XKLRBY9ydBWiBS2GxOXvFd0hLbCgTD0" +
            "PGKcy7uoicmyE2RaTZvxzlW31zSctub276NweszOjlOBugWiQLLNEoYKzfw0P-udwoH9Gqa4A5Ws4exTUIobu90Pl2Kde96MNSanvBI8f5Qci2" +
            "FruATuEVg6Vc-674iX1IWitGJwXX7GqjCajPkiE10S6MWe53x6N5Iyy0lseGehOH5okdIaoG7ZgHgF34p8BYSyZB9l4VPWVCw491celE04oDXkfcX" +
            "D59n95glBNdJth13IbeokcNk6VDXop7bLp3wWxiOEFFjzY8NpPxpxLrGIlkAh73tB1y8jBhhbS4KP3VZL7AE78DNM6B7G6zrnQ"

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            modules(challengeTestModule)
        }

    @Test
    fun `challenge not verified when token expired`() =
        runTest {
            val challengeResponseDto =
                ChallengeResponseDto(
                    "",
                    SENT_DOMAIN,
                    SENT_VERIFY_TOKEN,
                    accessToken,
                    "",
                    null,
                )
            val result = challengeVerifier.verify(challengeResponseDto, public, SENT_VERIFY_TOKEN, SENT_DOMAIN)
            assertTrue(result is ChallengeVerifier.Output.TokenExpired)
        }

    @Test
    fun `challenge not verified when wrong access token`() =
        runTest {
            val challengeResponseDto =
                ChallengeResponseDto(
                    "",
                    SENT_DOMAIN,
                    SENT_VERIFY_TOKEN,
                    "wrong access",
                    "",
                    null,
                )
            val result = challengeVerifier.verify(challengeResponseDto, public, SENT_VERIFY_TOKEN, SENT_DOMAIN)
            assertTrue(result is ChallengeVerifier.Output.Failure)
        }

    @Test
    fun `challenge not verified when wrong public key`() =
        runTest {
            val challengeResponseDto =
                ChallengeResponseDto(
                    "",
                    SENT_DOMAIN,
                    SENT_VERIFY_TOKEN,
                    accessToken,
                    "",
                    null,
                )
            val result = challengeVerifier.verify(challengeResponseDto, wrongPublic, SENT_VERIFY_TOKEN, SENT_DOMAIN)
            assertTrue(result is ChallengeVerifier.Output.InvalidSignature)
        }

    @Test
    fun `challenge not verified when returned verify token does not match the sent one`() =
        runTest {
            val challengeResponseDto =
                ChallengeResponseDto(
                    "",
                    SENT_DOMAIN,
                    "e1ebc592-b90d-5e22-9f40-50e52911673b",
                    accessToken,
                    "",
                    null,
                )
            val result = challengeVerifier.verify(challengeResponseDto, public, SENT_VERIFY_TOKEN, SENT_DOMAIN)
            assertTrue(result is ChallengeVerifier.Output.VerifyTokenMismatch)
        }

    @Test
    fun `challenge domain check passes when returned domain differs only by a trailing slash`() =
        runTest {
            val challengeResponseDto =
                ChallengeResponseDto(
                    "",
                    "$SENT_DOMAIN/",
                    SENT_VERIFY_TOKEN,
                    accessToken,
                    "",
                    null,
                )
            val result = challengeVerifier.verify(challengeResponseDto, public, SENT_VERIFY_TOKEN, SENT_DOMAIN)
            assertTrue(result is ChallengeVerifier.Output.TokenExpired)
        }

    @Test
    fun `challenge not verified when returned domain does not match the sent one`() =
        runTest {
            val challengeResponseDto =
                ChallengeResponseDto(
                    "",
                    "https://attacker.dev",
                    SENT_VERIFY_TOKEN,
                    accessToken,
                    "",
                    null,
                )
            val result = challengeVerifier.verify(challengeResponseDto, public, SENT_VERIFY_TOKEN, SENT_DOMAIN)
            assertTrue(result is ChallengeVerifier.Output.DomainMismatch)
        }

    private companion object {
        private const val SENT_VERIFY_TOKEN = "555a30f6-48f0-42be-beca-d200347f1848"
        private const val SENT_DOMAIN = "https://passbolt.dev"
    }
}
