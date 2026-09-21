package com.passbolt.mobile.android.core.networking.interceptor

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.networking.PLACEHOLDER_BASE_URL
import com.passbolt.mobile.android.domain.accounts.usecase.GetCurrentApiUrlUseCase
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

class ChangeableBaseUrlInterceptorTest {
    private val getCurrentApiUrlUseCase =
        mock<GetCurrentApiUrlUseCase> {
            on { execute(Unit) } doReturn GetCurrentApiUrlUseCase.Output(API_URL)
        }
    private val interceptor = ChangeableBaseUrlInterceptor(getCurrentApiUrlUseCase)

    @Test
    fun `should replace placeholder base url with the api url`() {
        val sent = intercept("$PLACEHOLDER_BASE_URL/resources.json")

        assertThat(sent.url.toString()).isEqualTo("$API_URL/resources.json")
    }

    @Test
    fun `should tag request with the api origin`() {
        val sent = intercept("$PLACEHOLDER_BASE_URL/resources.json")

        assertThat(sent.apiOrigin).isEqualTo(ApiOrigin(scheme = "https", host = "example.com", port = 443))
    }

    private fun intercept(url: String): Request {
        val chain =
            mock<Interceptor.Chain> {
                on { request() } doReturn Request.Builder().url(url).build()
                on { proceed(any()) } doAnswer { emptyResponse(it.getArgument(0)) }
            }

        interceptor.intercept(chain)

        return argumentCaptor<Request> { verify(chain).proceed(capture()) }.firstValue
    }

    private fun emptyResponse(request: Request) =
        Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody())
            .build()

    private companion object {
        private const val API_URL = "https://example.com/passbolt"
    }
}
