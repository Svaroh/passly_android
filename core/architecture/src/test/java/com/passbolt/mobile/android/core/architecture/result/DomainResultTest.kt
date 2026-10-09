package net.svaroh.passly.core.architecture.result

import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.SERVER
import org.junit.Test
import kotlin.test.assertEquals

class DomainResultTest {
    @Test
    fun `map transforms a Finished value`() {
        val result: DomainResult<Int> = DomainResult.Finished(2)

        assertEquals(DomainResult.Finished(6), result.map { it * 3 })
    }

    @Test
    fun `map leaves an Incomplete unchanged`() {
        val unauthorized: DomainResult<Int> = DomainResult.Incomplete.Unauthorized
        val error: DomainResult<Int> = DomainResult.Incomplete.Error(SERVER, "boom")

        assertEquals(DomainResult.Incomplete.Unauthorized, unauthorized.map { it * 3 })
        assertEquals(DomainResult.Incomplete.Error(SERVER, "boom"), error.map { it * 3 })
    }
}
