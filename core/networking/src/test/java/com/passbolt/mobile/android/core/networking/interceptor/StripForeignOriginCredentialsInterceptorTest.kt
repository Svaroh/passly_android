package net.svaroh.passly.core.networking.interceptor

import com.google.common.truth.Truth.assertThat
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class StripForeignOriginCredentialsInterceptorTest {
    private val interceptor = StripForeignOriginCredentialsInterceptor()

    @Test
    fun `should keep credentials on request to the api origin`() {
        val sent = intercept("$API_URL/resources.json", apiOrigin = API_ORIGIN)

        assertThat(sent.header(AUTHORIZATION_HEADER)).isEqualTo(AUTHORIZATION)
        assertThat(sent.header(COOKIE_HEADER)).isEqualTo(MFA_COOKIE)
    }

    @Test
    fun `should strip credentials on request to another host`() {
        val sent = intercept("https://other.example.com/passbolt/resources.json", apiOrigin = API_ORIGIN)

        assertThat(sent.header(AUTHORIZATION_HEADER)).isNull()
        assertThat(sent.header(COOKIE_HEADER)).isNull()
    }

    @Test
    fun `should strip credentials on request to the api host over another scheme or port`() {
        listOf(
            "http://example.com/passbolt/resources.json",
            "https://example.com:8443/passbolt/resources.json",
        ).forEach { url ->
            val sent = intercept(url, apiOrigin = API_ORIGIN)

            assertThat(sent.header(AUTHORIZATION_HEADER)).isNull()
            assertThat(sent.header(COOKIE_HEADER)).isNull()
        }
    }

    @Test
    fun `should strip credentials when api origin is unknown`() {
        val sent = intercept("$API_URL/resources.json", apiOrigin = null)

        assertThat(sent.header(AUTHORIZATION_HEADER)).isNull()
        assertThat(sent.header(COOKIE_HEADER)).isNull()
    }

    @Test
    fun `should keep other headers when stripping credentials`() {
        val sent = intercept("https://other.example.com/resources.json", apiOrigin = API_ORIGIN)

        assertThat(sent.header(ACCEPT_HEADER)).isEqualTo(ACCEPT_JSON)
    }

    @Test
    fun `should keep cookies set by a response from the api origin`() {
        val received = interceptResponse("$API_URL/resources.json", apiOrigin = API_ORIGIN)

        assertThat(received.headers(SET_COOKIE_HEADER))
            .containsExactly(MFA_SET_COOKIE, REFRESH_TOKEN_SET_COOKIE)
            .inOrder()
    }

    @Test
    fun `should drop cookies set by a response from another host`() {
        val received = interceptResponse("https://other.example.com/passbolt/resources.json", apiOrigin = API_ORIGIN)

        assertThat(received.headers(SET_COOKIE_HEADER)).isEmpty()
    }

    @Test
    fun `should drop cookies set by a response when api origin is unknown`() {
        val received = interceptResponse("$API_URL/resources.json", apiOrigin = null)

        assertThat(received.headers(SET_COOKIE_HEADER)).isEmpty()
    }

    @Test
    fun `should keep other response headers when dropping cookies`() {
        val received = interceptResponse("https://other.example.com/resources.json", apiOrigin = API_ORIGIN)

        assertThat(received.header(CONTENT_TYPE_HEADER)).isEqualTo(CONTENT_TYPE_JSON)
    }

    private fun intercept(
        url: String,
        apiOrigin: ApiOrigin?,
    ): Request {
        val chain = chain(url, apiOrigin)

        interceptor.intercept(chain)

        return argumentCaptor<Request> { verify(chain).proceed(capture()) }.firstValue
    }

    private fun interceptResponse(
        url: String,
        apiOrigin: ApiOrigin?,
    ): Response = interceptor.intercept(chain(url, apiOrigin))

    private fun chain(
        url: String,
        apiOrigin: ApiOrigin?,
    ): Interceptor.Chain {
        val request =
            Request
                .Builder()
                .url(url)
                .header(AUTHORIZATION_HEADER, AUTHORIZATION)
                .header(COOKIE_HEADER, MFA_COOKIE)
                .header(ACCEPT_HEADER, ACCEPT_JSON)
                .tagApiOrigin(apiOrigin)
                .build()
        return mock<Interceptor.Chain> {
            on { request() } doReturn request
            on { proceed(any()) } doAnswer { cookieSettingResponse(it.getArgument(0)) }
        }
    }

    private fun cookieSettingResponse(request: Request) =
        Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON)
            .addHeader(SET_COOKIE_HEADER, MFA_SET_COOKIE)
            .addHeader(SET_COOKIE_HEADER, REFRESH_TOKEN_SET_COOKIE)
            .body("".toResponseBody())
            .build()

    private companion object {
        private const val API_URL = "https://example.com/passbolt"
        private val API_ORIGIN = ApiOrigin(API_URL.toHttpUrl())
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val AUTHORIZATION = "Bearer access-token"
        private const val COOKIE_HEADER = "Cookie"
        private const val MFA_COOKIE = "passbolt_mfa=mfa-jwt"
        private const val ACCEPT_HEADER = "Accept"
        private const val ACCEPT_JSON = "application/json"
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private const val CONTENT_TYPE_JSON = "application/json"
        private const val SET_COOKIE_HEADER = "Set-Cookie"
        private const val MFA_SET_COOKIE = "passbolt_mfa=mfa-jwt; Path=/; HttpOnly"
        private const val REFRESH_TOKEN_SET_COOKIE = "refresh_token=refresh-token; Path=/auth; HttpOnly"
    }
}
