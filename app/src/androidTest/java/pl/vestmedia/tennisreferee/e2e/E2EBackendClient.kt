package pl.vestmedia.tennisreferee.e2e

import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.MediaType.Companion.toMediaType
import pl.vestmedia.tennisreferee.data.auth.CourtSession
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider
import pl.vestmedia.tennisreferee.data.auth.parseSessionExpiry
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Admin/E2E HTTP client for instrumentation tests.
 *
 * Base URL resolution order:
 * 1. instrumentation arg `e2e.baseUrl`
 * 2. env `E2E_BASE_URL` / `e2e.baseUrl`
 * 3. default `http://10.0.2.2:18087` (emulator → host Docker e2e)
 *
 * Admin password:
 * 1. instrumentation arg `e2e.adminPassword`
 * 2. env `E2E_ADMIN_PASSWORD`
 * 3. default `e2e-admin` (docker-compose.e2e.yml)
 */
class E2EBackendClient(
    baseUrl: String = resolveBaseUrl()
) {
    val baseUrl: String = baseUrl.trimEnd('/')

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var adminToken: String? = null

    fun createTournamentFixture(marker: String): TournamentFixture {
        ensureAdminToken()
        val today = LocalDate.now()
        try {
            val tournament = postJson(
                "/admin/api/tournaments",
                JSONObject()
                    .put("name", "$marker Android Emulator Open")
                    .put("start_date", today.toString())
                    .put("end_date", today.plusDays(1).toString())
                    .put("active", true)
                    .put("is_simulation", true)
                    .put("office_password", "test")
                    .put("city", "E2E")
                    .put("country", "PL")
                    .put("court_count", 8)
            )
            val tournamentId = when {
                tournament.has("id") -> tournament.getInt("id")
                tournament.has("tournament") -> tournament.getJSONObject("tournament").getInt("id")
                else -> error("Tournament create response missing id: $tournament")
            }
            val players = createPlayers(marker, tournamentId)
            putJson(
                "/admin/api/tournaments/$tournamentId/bracket/groups",
                JSONObject().put(
                    "groups",
                    JSONArray()
                        .put(JSONObject().put("name", "Group A").put("players", JSONArray(listOf(players[0].id, players[1].id, players[2].id, players[3].id))))
                        .put(JSONObject().put("name", "Group B").put("players", JSONArray(listOf(players[4].id, players[5].id, players[6].id, players[7].id))))
                )
            )
            putJson(
                "/admin/api/tournaments/$tournamentId/bracket/knockout",
                JSONObject().put(
                    "knockout",
                    JSONArray()
                        .put(JSONObject().put("phase", "semifinal").put("position", 1).put("player1_name", players[0].fullName).put("player2_name", players[4].fullName))
                        .put(JSONObject().put("phase", "semifinal").put("position", 2).put("player1_name", players[1].fullName).put("player2_name", players[5].fullName))
                        .put(JSONObject().put("phase", "final").put("position", 1))
                        .put(JSONObject().put("phase", "third_place").put("position", 1))
                )
            )
            return TournamentFixture(marker, tournamentId, players)
        } catch (error: Throwable) {
            cleanup(marker)
            throw error
        }
    }

    fun loadTournamentFixture(marker: String, tournamentId: Int): TournamentFixture {
        ensureAdminToken()
        val playersJson = getJsonArray("/admin/api/tournaments/$tournamentId/players")
        val players = mutableListOf<E2EPlayer>()
        for (index in 0 until playersJson.length()) {
            val row = playersJson.getJSONObject(index)
            players += E2EPlayer(
                id = row.getInt("id"),
                firstName = row.optString("first_name"),
                lastName = row.optString("last_name"),
                gender = row.optString("gender"),
                country = row.optString("country")
            )
        }
        val ordered = players.sortedWith(
            compareBy<E2EPlayer> { playerOrdinal(it.lastName) }.thenBy { it.lastName }.thenBy { it.firstName }
        )
        require(ordered.size >= 8) {
            "Expected at least 8 players for marker=$marker tournamentId=$tournamentId, got ${ordered.size}"
        }
        return TournamentFixture(marker, tournamentId, ordered)
    }

    fun cleanup(marker: String) {
        ensureAdminToken(allowFailure = true)
        postJson("/admin/api/e2e/cleanup", JSONObject().put("marker", marker), allowFailure = true)
    }

    /**
     * Seeds a doubles category, two pairs, one RR slot on [courtId], and publishes it
     * so the umpire suggestion card can apply Debel without dropping scheduleId.
     */
    fun seedDoublesSuggestedMatch(fixture: TournamentFixture, courtId: String) {
        ensureAdminToken()
        val today = LocalDate.now().toString()
        val now = java.time.LocalTime.now().withSecond(0).withNano(0).toString().take(5)
        val category = postJson(
            "/admin/api/tournaments/${fixture.tournamentId}/categories",
            JSONObject().put("label", "B1 Double").put("is_doubles", true)
        )
        val categoryId = when {
            category.has("id") -> category.getInt("id")
            category.has("category") -> category.getJSONObject("category").getInt("id")
            else -> error("Category create response missing id: $category")
        }
        val teamA = createTeam(fixture.tournamentId, categoryId, fixture.players[0].id, fixture.players[1].id)
        val teamB = createTeam(fixture.tournamentId, categoryId, fixture.players[2].id, fixture.players[3].id)
        putJson(
            "/admin/api/tournaments/${fixture.tournamentId}/bracket/groups",
            JSONObject().put(
                "groups",
                JSONArray().put(
                    JSONObject()
                        .put("name", "B1 Double — Grupa A")
                        .put("tournament_category_id", categoryId)
                        .put("play_format", "round_robin")
                        .put("teams", JSONArray(listOf(teamA, teamB)))
                )
            )
        )
        val schedulePayload = getJson("/admin/api/tournaments/${fixture.tournamentId}/schedule")
        val entries = schedulePayload.optJSONArray("schedule") ?: JSONArray()
        require(entries.length() >= 1) { "Expected generated doubles schedule, got $schedulePayload" }
        val scheduleId = entries.getJSONObject(0).getInt("id")
        putJson(
            "/admin/api/tournaments/${fixture.tournamentId}/schedule/$scheduleId",
            JSONObject()
                .put("day_date", today)
                .put("scheduled_time", now)
                .put("court_id", courtId)
                .put("status", "planned")
        )
    }

    private fun createTeam(tournamentId: Int, categoryId: Int, player1Id: Int, player2Id: Int): Int {
        val response = postJson(
            "/admin/api/tournaments/$tournamentId/teams",
            JSONObject()
                .put("category_id", categoryId)
                .put("player1_id", player1Id)
                .put("player2_id", player2Id)
        )
        return when {
            response.has("id") -> response.getInt("id")
            response.has("team") -> response.getJSONObject("team").getInt("id")
            else -> error("Team create response missing id: $response")
        }
    }

    /** Returns court PIN (default tournament courts use 0000). */
    fun fetchCourtPin(courtId: String): String {
        ensureAdminToken()
        val courts = getJsonArray("/admin/api/courts")
        for (index in 0 until courts.length()) {
            val row = courts.getJSONObject(index)
            if (row.optString("kort_id") == courtId || row.optString("id") == courtId) {
                val pin = row.optString("pin").takeIf { it.isNotBlank() } ?: "0000"
                return pin
            }
        }
        return "0000"
    }

    fun setCourtPin(courtId: String, pin: String) {
        ensureAdminToken()
        putJson("/admin/api/courts/$courtId/pin", JSONObject().put("pin", pin))
    }

    fun authorizeCourt(courtId: String, pin: String): CourtSession {
        val response = postJsonUnauthed(
            "/api/courts/$courtId/authorize",
            JSONObject().put("pin", pin)
        )
        if (!response.optBoolean("authorized", false) && !response.optBoolean("ok", false)) {
            error("Court $courtId was not authorized: $response")
        }
        val token = response.optString("token").takeIf { it.isNotBlank() }
            ?: error("Court $courtId authorize response missing token: $response")
        val expiresRaw = sequenceOf("expires_at", "expiresAt", "expiry")
            .map { response.optString(it) }
            .firstOrNull { it.isNotBlank() }
        return CourtSession(
            courtId = response.optString("court_id").ifBlank {
                response.optString("kort_id")
            }.ifBlank { courtId },
            token = token,
            expiresAtMillis = parseSessionExpiry(expiresRaw)
        )
    }

    /** Court-auth grace ended; MatchActivity sync needs a bearer session. */
    fun seedAppCourtSession(courtId: String, pin: String = "4242") {
        setCourtPin(courtId, pin)
        CourtSessionProvider.get().save(authorizeCourt(courtId, pin))
    }

    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        client.cache?.close()
    }

    fun waitForInProgressMatch(marker: String, player1Name: String, player2Name: String) {
        waitUntil("server in-progress match for $player1Name vs $player2Name", timeoutMs = 30_000) {
            val match = findMatch(fetchArtifacts(marker).getJSONArray("matches"), player1Name, player2Name)
            match?.optString("status") == "in_progress"
        }
    }

    fun waitForFinishedArtifacts(marker: String, player1Name: String, player2Name: String): FinishedArtifacts {
        var result: FinishedArtifacts? = null
        waitUntil("finished server artifacts for $player1Name vs $player2Name", timeoutMs = 60_000) {
            val artifacts = fetchArtifacts(marker)
            val match = findMatch(artifacts.getJSONArray("matches"), player1Name, player2Name)
            if (match?.optString("status") != "finished") return@waitUntil false
            val matchId = match.getInt("id")
            val history = findByMatchId(artifacts.getJSONArray("history"), matchId) ?: return@waitUntil false
            val statistics = findByMatchId(artifacts.getJSONArray("statistics"), matchId) ?: return@waitUntil false
            result = FinishedArtifacts(match, history, statistics)
            true
        }
        return requireNotNull(result)
    }

    private fun ensureAdminToken(allowFailure: Boolean = false) {
        if (!adminToken.isNullOrBlank()) return
        val password = resolveAdminPassword()
        try {
            val response = postJsonUnauthed(
                "/admin/api/auth",
                JSONObject().put("password", password)
            )
            adminToken = response.optString("token").takeIf { it.isNotBlank() }
                ?: error("Admin auth missing token")
        } catch (error: Throwable) {
            if (!allowFailure) throw error
        }
    }

    private fun createPlayers(marker: String, tournamentId: Int): List<E2EPlayer> {
        val definitions = listOf(
            Triple("Ana", "F", "PL"),
            Triple("Bartosz", "M", "PL"),
            Triple("Celina", "F", "CZ"),
            Triple("Dominik", "M", "DE"),
            Triple("Elena", "F", "ES"),
            Triple("Filip", "M", "FR"),
            Triple("Gaja", "F", "IT"),
            Triple("Hubert", "M", "GB")
        )
        return definitions.mapIndexed { index, definition ->
            val firstName = definition.first
            val lastName = "$marker-P${index + 1}"
            val response = postJson(
                "/admin/api/tournaments/$tournamentId/players",
                JSONObject()
                    .put("first_name", firstName)
                    .put("last_name", lastName)
                    .put("name", "$firstName $lastName")
                    .put("gender", definition.second)
                    .put("country", definition.third)
                    .put("category", "E2E")
            )
            E2EPlayer(response.getInt("id"), firstName, lastName, definition.second, definition.third)
        }
    }

    private fun fetchArtifacts(marker: String): JSONObject {
        ensureAdminToken()
        return getJson("/admin/api/e2e/artifacts?marker=${URLEncoder.encode(marker, "UTF-8")}")
    }

    private fun findMatch(matches: JSONArray, player1Name: String, player2Name: String): JSONObject? {
        for (index in 0 until matches.length()) {
            val match = matches.getJSONObject(index)
            if (match.optString("player1_name") == player1Name && match.optString("player2_name") == player2Name) {
                return match
            }
        }
        return null
    }

    private fun findByMatchId(items: JSONArray, matchId: Int): JSONObject? {
        for (index in 0 until items.length()) {
            val item = items.getJSONObject(index)
            if (item.optInt("match_id") == matchId) {
                return item
            }
        }
        return null
    }

    private fun getJson(path: String): JSONObject = requestObject("GET", path, null)
    private fun getJsonArray(path: String): JSONArray = requestArray("GET", path)
    private fun postJson(path: String, body: JSONObject, allowFailure: Boolean = false): JSONObject =
        requestObject("POST", path, body, allowFailure)
    private fun putJson(path: String, body: JSONObject): JSONObject = requestObject("PUT", path, body)

    private fun postJsonUnauthed(path: String, body: JSONObject): JSONObject {
        val text = execute("POST", path, body, allowFailure = false, withAuth = false)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun requestObject(
        method: String,
        path: String,
        body: JSONObject?,
        allowFailure: Boolean = false
    ): JSONObject {
        val text = execute(method, path, body, allowFailure, withAuth = true)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun requestArray(method: String, path: String): JSONArray {
        val text = execute(method, path, null, allowFailure = false, withAuth = true)
        return if (text.isBlank()) JSONArray() else JSONArray(text)
    }

    private fun execute(
        method: String,
        path: String,
        body: JSONObject?,
        allowFailure: Boolean,
        withAuth: Boolean
    ): String {
        val builder = Request.Builder().url("$baseUrl$path")
        if (withAuth) {
            val token = adminToken
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
        }
        val request = builder
            .method(method, if (method == "GET") null else (body ?: JSONObject()).toString().toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!allowFailure && !response.isSuccessful) {
                throw AssertionError("$method $path failed: HTTP ${response.code} $text (baseUrl=$baseUrl)")
            }
            return text
        }
    }

    companion object {
        fun resolveBaseUrl(): String {
            val args = runCatching { InstrumentationRegistry.getArguments() }.getOrNull()
            val fromArg = args?.getString("e2e.baseUrl")?.trim()?.takeIf { it.isNotEmpty() }
            if (fromArg != null) return fromArg.trimEnd('/')

            val fromEnv = sequenceOf("E2E_BASE_URL", "e2e.baseUrl")
                .mapNotNull { key -> System.getenv(key)?.trim()?.takeIf { it.isNotEmpty() } }
                .firstOrNull()
            if (fromEnv != null) return fromEnv.trimEnd('/')

            return "http://10.0.2.2:18087"
        }

        fun resolveAdminPassword(): String {
            val fromArg = instrumentationArg("e2e.adminPassword")
            if (fromArg != null) return fromArg
            val fromEnv = System.getenv("E2E_ADMIN_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() }
            if (fromEnv != null) return fromEnv
            return "e2e-admin"
        }

        fun instrumentationArg(name: String): String? =
            runCatching { InstrumentationRegistry.getArguments().getString(name) }
                .getOrNull()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        private fun playerOrdinal(lastName: String): Int {
            val match = Regex("""-P(\d+)$""").find(lastName)
            return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
        }
    }
}
