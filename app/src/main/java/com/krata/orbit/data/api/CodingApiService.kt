package com.krata.orbit.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

private const val TAG = "CodingApiService"

data class ApiPotd(
    val site: String,
    val problemName: String,
    val difficulty: String,
    val url: String
)

data class ApiContest(
    val site: String,
    val title: String,
    val url: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val coverImage: String = ""
)

object CodingApiService {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    // ── LeetCode POTD ─────────────────────────────────────────────────────────
    suspend fun fetchLeetCodePotd(): ApiPotd? = withContext(Dispatchers.IO) {
        try {
            val query = """
                query questionOfToday {
                    activeDailyCodingChallengeQuestion {
                        date
                        link
                        question {
                            difficulty
                            title
                            titleSlug
                        }
                    }
                }
            """.trimIndent()

            val body = buildJsonObject {
                put("query", query)
                put("variables", buildJsonObject {})
                put("operationName", "questionOfToday")
            }.toString()

            val request = Request.Builder()
                .url("https://leetcode.com/graphql")
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Referer", "https://leetcode.com")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            val root = json.parseToJsonElement(responseBody).jsonObject

            val q = root["data"]?.jsonObject
                ?.get("activeDailyCodingChallengeQuestion")?.jsonObject
                ?: return@withContext null

            val question = q["question"]?.jsonObject ?: return@withContext null
            val title     = question["title"]?.jsonPrimitive?.content ?: "Unknown"
            val slug      = question["titleSlug"]?.jsonPrimitive?.content ?: ""
            val difficulty = question["difficulty"]?.jsonPrimitive?.content ?: "Medium"
            val link      = q["link"]?.jsonPrimitive?.content ?: "/problems/$slug/"

            ApiPotd(
                site        = "leetcode",
                problemName = title,
                difficulty  = difficulty,
                url         = "https://leetcode.com$link"
            )
        } catch (e: Exception) {
            Log.e(TAG, "LeetCode POTD error: ${e.message}")
            null
        }
    }

    // ── GFG POTD ──────────────────────────────────────────────────────────────
    suspend fun fetchGfgPotd(): ApiPotd? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://practiceapi.geeksforgeeks.org/api/v1/problems-of-day/problem/today/")
                .get()
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            val root = json.parseToJsonElement(responseBody).jsonObject

            val problem = root["problem_of_the_day"]?.jsonObject ?: root

            val title      = problem["problem_name"]?.jsonPrimitive?.content
                ?: problem["title"]?.jsonPrimitive?.content
                ?: "Unknown"
            val difficulty = problem["difficulty_level"]?.jsonPrimitive?.content
                ?: problem["difficulty"]?.jsonPrimitive?.content
                ?: "Medium"
            val slug       = problem["problem_url"]?.jsonPrimitive?.content
                ?: problem["url"]?.jsonPrimitive?.content
                ?: ""

            val url = if (slug.startsWith("http")) slug
                      else "https://www.geeksforgeeks.org/problems/$slug"

            ApiPotd(
                site        = "gfg",
                problemName = title,
                difficulty  = difficulty,
                url         = url
            )
        } catch (e: Exception) {
            Log.e(TAG, "GFG POTD error: ${e.message}")
            null
        }
    }

    // ── LeetCode Contests ─────────────────────────────────────────────────────
    suspend fun fetchLeetCodeContests(): List<ApiContest> = withContext(Dispatchers.IO) {
        try {
            val query = """
                query contestV2UpcomingContests {
                    contestV2UpcomingContests {
                        titleSlug
                        title
                        startTime
                        duration
                        cardImg
                    }
                }
            """.trimIndent()

            val body = buildJsonObject {
                put("query", query)
                put("variables", buildJsonObject {})
                put("operationName", "contestV2UpcomingContests")
            }.toString()

            val request = Request.Builder()
                .url("https://leetcode.com/graphql")
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Referer", "https://leetcode.com")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val root = json.parseToJsonElement(responseBody).jsonObject

            val contests = root["data"]?.jsonObject
                ?.get("contestV2UpcomingContests")?.jsonArray
                ?: return@withContext emptyList()

            contests.mapNotNull { el ->
                val c = el.jsonObject
                val titleSlug  = c["titleSlug"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val title      = c["title"]?.jsonPrimitive?.content ?: titleSlug
                val startTime  = c["startTime"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                val duration   = c["duration"]?.jsonPrimitive?.longOrNull ?: 5400L
                val cardImg    = c["cardImg"]?.jsonPrimitive?.content ?: ""
                ApiContest(
                    site            = "leetcode",
                    title           = title,
                    url             = "https://leetcode.com/contest/$titleSlug",
                    startTimeMillis = startTime * 1000L,
                    endTimeMillis   = (startTime + duration) * 1000L,
                    coverImage      = cardImg
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "LeetCode contests error: ${e.message}")
            emptyList()
        }
    }

    // ── CodeChef Contests ─────────────────────────────────────────────────────
    suspend fun fetchCodeChefContests(): List<ApiContest> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://www.codechef.com/api/list/contests/all?sort_by=START&sorting_order=asc&offset=0&mode=all")
                .get()
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val root = json.parseToJsonElement(responseBody).jsonObject

            fun parseContestArray(arr: JsonArray?): List<ApiContest> =
                arr?.mapNotNull { el ->
                    val c = el.jsonObject
                    val name      = c["contest_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val code      = c["contest_code"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val startIso  = c["contest_start_date_iso"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val endIso    = c["contest_end_date_iso"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val startMs   = parseIso8601(startIso) ?: return@mapNotNull null
                    val endMs     = parseIso8601(endIso) ?: return@mapNotNull null
                    ApiContest(
                        site            = "codechef",
                        title           = name,
                        url             = "https://codechef.com/$code",
                        startTimeMillis = startMs,
                        endTimeMillis   = endMs
                    )
                } ?: emptyList()

            parseContestArray(root["present_contests"]?.jsonArray) +
            parseContestArray(root["future_contests"]?.jsonArray)
        } catch (e: Exception) {
            Log.e(TAG, "CodeChef contests error: ${e.message}")
            emptyList()
        }
    }

    // ── Codeforces Contests ───────────────────────────────────────────────────
    suspend fun fetchCodeforcesContests(): List<ApiContest> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://codeforces.com/api/contest.list?gym=false")
                .get()
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val root = json.parseToJsonElement(responseBody).jsonObject

            val validPhases = setOf("BEFORE", "CODING")
            val results = root["result"]?.jsonArray ?: return@withContext emptyList()

            results.mapNotNull { el ->
                val c      = el.jsonObject
                val phase  = c["phase"]?.jsonPrimitive?.content ?: return@mapNotNull null
                if (phase !in validPhases) return@mapNotNull null
                val id       = c["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                val name     = c["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val startSec = c["startTimeSeconds"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                val durSec   = c["durationSeconds"]?.jsonPrimitive?.longOrNull ?: 7200L
                ApiContest(
                    site            = "codeforces",
                    title           = name,
                    url             = "https://codeforces.com/contest/$id",
                    startTimeMillis = startSec * 1000L,
                    endTimeMillis   = (startSec + durSec) * 1000L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Codeforces contests error: ${e.message}")
            emptyList()
        }
    }

    // ── ISO 8601 parser helper ────────────────────────────────────────────────
    private fun parseIso8601(iso: String): Long? = try {
        java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli()
    } catch (_: Exception) {
        try {
            java.time.LocalDateTime.parse(iso.replace(" ", "T"))
                .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        } catch (_: Exception) { null }
    }
}
