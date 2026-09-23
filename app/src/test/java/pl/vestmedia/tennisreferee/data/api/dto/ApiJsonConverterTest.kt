package pl.vestmedia.tennisreferee.data.api.dto

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class ApiJsonConverterTest {

    private val retrofit = Retrofit.Builder().baseUrl("http://localhost/").build()
    private val factory = ApiJsonConverterFactory()

    @Test
    fun listOfTournamentsIgnoresUnknownKeys() {
        val type = object : TypeToken<List<TournamentOptionDto>>() {}.type
        val converter = factory.responseBodyConverter(type, emptyAnnotations(), retrofit)
        val body = """[{"id":32,"name":"RAKIETY ATNiS VII","is_public":1}]"""
            .toResponseBody(JSON)
        @Suppress("UNCHECKED_CAST")
        val parsed = converter!!.convert(body) as List<TournamentOptionDto>
        assertEquals(32, parsed.single().id)
        assertEquals("RAKIETY ATNiS VII", parsed.single().name)
    }

    @Test
    fun mapAndUnitAreLeftForGson() {
        val mapType: Type = object : TypeToken<Map<String, String>>() {}.type
        assertNull(factory.requestBodyConverter(mapType, emptyAnnotations(), emptyAnnotations(), retrofit))
        assertNull(factory.responseBodyConverter(Unit::class.java, emptyAnnotations(), retrofit))
    }

    @Test
    fun playerBodyKeepsThePrimaryName() {
        val converter = factory.requestBodyConverter(
            PlayerDto::class.java,
            emptyAnnotations(),
            emptyAnnotations(),
            retrofit,
        )
        val body = (converter as retrofit2.Converter<Any, RequestBody>).convert(
            PlayerDto(id = 1, name = "Kowalski"),
        )!!
        val buffer = Buffer()
        body.writeTo(buffer)
        val json = buffer.readUtf8()
        assertTrue(json.contains("\"name\":\"Kowalski\""))
        assertFalse(json.contains("surname"))
    }

    private fun emptyAnnotations() = emptyArray<Annotation>()

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
