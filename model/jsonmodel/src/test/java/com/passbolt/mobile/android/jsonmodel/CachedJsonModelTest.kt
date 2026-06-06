package net.svaroh.passly.jsonmodel

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.jsonmodel.delegates.RootRelativeJsonPathNullableStringDelegate
import net.svaroh.passly.jsonmodel.delegates.RootRelativeJsonPathStringDelegate
import net.svaroh.passly.jsonmodel.delegates.jsonPathDelegatesTestModule
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

class CachedJsonModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(jsonPathDelegatesTestModule)
        }

    private class TestCachedModel(
        initialJson: String?,
    ) : CachedJsonModel {
        override var json: String? = initialJson
        override var parsedCache: ParsedJson? = null
        var field: String by RootRelativeJsonPathStringDelegate(jsonPath = "field")
        var other: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "other")
    }

    @Test
    fun `repeated reads reuse the same parsed document`() {
        val model = TestCachedModel("""{ "field": "a" }""")

        assertThat(model.field).isEqualTo("a")
        val afterFirstRead = model.parsedCache
        assertThat(afterFirstRead).isNotNull()

        assertThat(model.field).isEqualTo("a")
        assertThat(model.parsedCache).isSameInstanceAs(afterFirstRead)
    }

    @Test
    fun `reassigning json invalidates the cache`() {
        val model = TestCachedModel("""{ "field": "a" }""")
        assertThat(model.field).isEqualTo("a")
        val firstCache = model.parsedCache

        model.json = """{ "field": "b" }"""

        assertThat(model.field).isEqualTo("b")
        assertThat(model.parsedCache).isNotSameInstanceAs(firstCache)
        assertThat(model.parsedCache!!.sourceJson).isSameInstanceAs(model.json)
    }

    @Test
    fun `writing a field refreshes the cache to match the new json`() {
        val model = TestCachedModel("""{ "field": "a" }""")
        assertThat(model.field).isEqualTo("a")

        model.field = "c"

        assertThat(model.field).isEqualTo("c")
        assertThat(model.parsedCache!!.sourceJson).isSameInstanceAs(model.json)
    }

    @Test
    fun `reading a field after writing it returns the new value, not the cached one`() {
        val model = TestCachedModel("""{ "field": "a" }""")
        assertThat(model.field).isEqualTo("a")

        model.field = "z"

        assertThat(model.field).isEqualTo("z")
    }

    @Test
    fun `each field write replaces the cached parse`() {
        val model = TestCachedModel("""{ "field": "a" }""")
        assertThat(model.field).isEqualTo("a")
        val beforeWrite = model.parsedCache

        model.field = "b"
        val afterFirstWrite = model.parsedCache
        assertThat(afterFirstWrite).isNotSameInstanceAs(beforeWrite)
        assertThat(afterFirstWrite!!.sourceJson).isSameInstanceAs(model.json)

        model.field = "c"
        val afterSecondWrite = model.parsedCache
        assertThat(afterSecondWrite).isNotSameInstanceAs(afterFirstWrite)
        assertThat(afterSecondWrite!!.sourceJson).isSameInstanceAs(model.json)
        assertThat(model.field).isEqualTo("c")
    }

    @Test
    fun `writing one field does not clobber another after the cache is warm`() {
        val model = TestCachedModel("""{ "field": "a", "other": "keep" }""")
        assertThat(model.field).isEqualTo("a")
        assertThat(model.other).isEqualTo("keep")

        model.field = "changed"

        assertThat(model.field).isEqualTo("changed")
        assertThat(model.other).isEqualTo("keep")
    }

    @Test
    fun `creating a new field via setter invalidates the warm cache and is readable`() {
        val model = TestCachedModel("""{ "field": "a" }""")
        assertThat(model.field).isEqualTo("a")
        assertThat(model.other).isNull()

        model.other = "created"

        assertThat(model.other).isEqualTo("created")
        assertThat(model.field).isEqualTo("a")
        assertThat(model.parsedCache!!.sourceJson).isSameInstanceAs(model.json)
    }
}
