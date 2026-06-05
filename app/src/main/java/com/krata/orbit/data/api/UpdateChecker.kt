package com.krata.orbit.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private const val TAG = "UpdateChecker"

// Replace this URL with your real GitHub releases API endpoint when publishing.
// Format: https://api.github.com/repos/{owner}/{repo}/releases/latest
private const val GITHUB_RELEASES_URL =
    "https://api.github.com/repos/krata-dev/orbit-android/releases/latest"

data class UpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val hasUpdate: Boolean
)

object UpdateChecker {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(GITHUB_RELEASES_URL)
                    .addHeader("Accept", "application/vnd.github+json")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null

                val body = response.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(body).jsonObject

                val tagName    = root["tag_name"]?.jsonPrimitive?.content ?: return@withContext null
                val htmlUrl    = root["html_url"]?.jsonPrimitive?.content ?: GITHUB_RELEASES_URL
                val latest     = tagName.trimStart('v')
                val hasUpdate  = isNewerVersion(latest, currentVersionName.trimStart('v'))

                UpdateInfo(
                    latestVersion = latest,
                    releaseUrl    = htmlUrl,
                    hasUpdate     = hasUpdate
                )
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed: ${e.message}")
                null
            }
        }

    /** Returns true if remote version is strictly newer than local. */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts  = local.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
