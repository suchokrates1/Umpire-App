package pl.vestmedia.tennisreferee.data.api.dto

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializerOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * kotlinx for API DTOs and lists of them. Gson stays behind this factory for
 * [Map] bodies and empty [Unit] responses, which this factory does not claim.
 */
@OptIn(ExperimentalSerializationApi::class)
class ApiJsonConverterFactory(
    private val json: kotlinx.serialization.json.Json = apiJson,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? {
        val serializer = serializerOrSkip(type) ?: return null
        return Converter { body ->
            val text = body.string()
            if (text.isBlank()) null else json.decodeFromString(serializer, text)
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody>? {
        val serializer = serializerOrSkip(type) ?: return null
        @Suppress("UNCHECKED_CAST")
        val strategy = serializer as SerializationStrategy<Any>
        return Converter<Any, RequestBody> { value ->
            json.encodeToString(strategy, value).toRequestBody(JSON)
        }
    }

    private fun serializerOrSkip(type: Type): KSerializer<Any>? {
        val raw = rawClass(type) ?: return null
        if (raw == Unit::class.java || Map::class.java.isAssignableFrom(raw)) return null
        return json.serializersModule.serializerOrNull(type)
    }

    private fun rawClass(type: Type): Class<*>? = when (type) {
        is Class<*> -> type
        is ParameterizedType -> rawClass(type.rawType)
        is WildcardType -> type.upperBounds.firstOrNull()?.let { rawClass(it) }
        else -> null
    }

    private companion object {
        val JSON = "application/json; charset=UTF-8".toMediaType()
    }
}
