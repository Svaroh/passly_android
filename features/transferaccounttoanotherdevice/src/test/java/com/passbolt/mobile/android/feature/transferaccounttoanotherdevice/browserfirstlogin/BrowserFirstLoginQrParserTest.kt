package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class BrowserFirstLoginQrParserTest {
    private val parser = BrowserFirstLoginQrParser(Json)

    @Test
    fun `parses browser first login deep link`() {
        val page =
            parser.parse(
                "passbolt://browser-first-login" +
                    "?type=browser_first_login" +
                    "&version=1" +
                    "&domain=https%3A%2F%2Fpass.66ton99.org.ua" +
                    "&request_id=79dce172-8b3c-4be5-a258-3129230996dd" +
                    "&secret=pairing-secret",
            )

        assertThat(page?.type).isEqualTo("browser_first_login")
        assertThat(page?.version).isEqualTo(1)
        assertThat(page?.domain).isEqualTo("https://pass.66ton99.org.ua")
        assertThat(page?.requestId).isEqualTo("79dce172-8b3c-4be5-a258-3129230996dd")
        assertThat(page?.secret).isEqualTo("pairing-secret")
    }

    @Test
    fun `parses browser first login android intent link`() {
        val page =
            parser.parse(
                "intent://browser-first-login" +
                    "?type=browser_first_login" +
                    "&version=1" +
                    "&domain=https%3A%2F%2Fpass.66ton99.org.ua" +
                    "&request_id=79dce172-8b3c-4be5-a258-3129230996dd" +
                    "&secret=pairing-secret" +
                    "#Intent;scheme=passbolt;package=com.passbolt.mobile.android;" +
                    "S.browser_fallback_url=https%3A%2F%2Fpass.66ton99.org.ua;end",
            )

        assertThat(page?.type).isEqualTo("browser_first_login")
        assertThat(page?.version).isEqualTo(1)
        assertThat(page?.domain).isEqualTo("https://pass.66ton99.org.ua")
        assertThat(page?.requestId).isEqualTo("79dce172-8b3c-4be5-a258-3129230996dd")
        assertThat(page?.secret).isEqualTo("pairing-secret")
    }

    @Test
    fun `parses legacy reserved-bytes browser first login QR payload`() {
        val page =
            parser.parse(
                "300{" +
                    "\"type\":\"browser_first_login\"," +
                    "\"version\":1," +
                    "\"domain\":\"https://pass.66ton99.org.ua\"," +
                    "\"request_id\":\"79dce172-8b3c-4be5-a258-3129230996dd\"," +
                    "\"secret\":\"pairing-secret\"" +
                    "}",
            )

        assertThat(page?.type).isEqualTo("browser_first_login")
        assertThat(page?.version).isEqualTo(1)
        assertThat(page?.domain).isEqualTo("https://pass.66ton99.org.ua")
        assertThat(page?.requestId).isEqualTo("79dce172-8b3c-4be5-a258-3129230996dd")
        assertThat(page?.secret).isEqualTo("pairing-secret")
    }
}
