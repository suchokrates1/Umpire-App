package pl.vestmedia.tennisreferee.data.api.dto

import kotlinx.serialization.json.Json

/**
 * Same bytes as Gson for the API DTOs: unknown keys are ignored, nulls are omitted,
 * and default values stay in the body.
 */
val apiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
