package pl.vestmedia.tennisreferee.data

import kotlinx.serialization.json.Json

/**
 * What the tablet writes to its own disk: the match it can restore after a crash, and
 * the rows Room keeps.
 *
 * Gson wrote these until 1.0.0-dev.51, with field names R8 was told to leave alone.
 * The shape is the same, so anything written by an older build still reads; new writes
 * no longer depend on a keep rule holding.
 */
val localJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
