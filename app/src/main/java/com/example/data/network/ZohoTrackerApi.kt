package com.example.data.network

import com.example.data.model.ZohoAuthResult
import com.example.data.model.ZohoPostResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ZohoTrackerApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build(),
) {

    companion object {
        const val AUTH_API_URL =
            "https://www.zohoapis.in/creator/custom/vishnu.b.2026_rkmshome/auth_user_by_email?publickey=h7ub2x7C0RXXkZ3bWK5EMvBRk"
        const val POST_API_URL =
            "https://www.zohoapis.in/creator/custom/vishnu.b.2026_rkmshome/post_location_data?publickey=jvztS3mph9pmDgKCp9yqt5Gue"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /**
         * Safely unwraps Zoho Creator custom API responses where the actual JSON payload
         * may be serialized as a string inside raw.result.
         */
        internal fun parseZohoPayload(rawBody: String): JSONObject {
            val trimmedBody = rawBody.trim()
            if (trimmedBody.isEmpty()) return JSONObject()
            val raw = JSONObject(trimmedBody)
            return when (val resultField = raw.opt("result")) {
                is String -> {
                    val inner = resultField.trim()
                    if (inner.startsWith("{")) JSONObject(inner) else raw
                }
                is JSONObject -> resultField
                else -> raw
            }
        }
    }

    suspend fun authenticateByEmail(email: String): ZohoAuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("email", email.trim())
        }
        val request = Request.Builder()
            .url(AUTH_API_URL)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (bodyText.isBlank()) {
                    return@withContext ZohoAuthResult.Error(
                        "Empty response from server (HTTP ${response.code})."
                    )
                }

                val parsed = parseZohoPayload(bodyText)
                val status = parsed.optString("status", "")

                if (status.equals("success", ignoreCase = true)) {
                    val userId = parsed.opt("user_id")
                        ?.toString()
                        ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    val name = parsed.optString("name", "")
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                        ?: email.substringBefore("@")

                    if (userId != null) {
                        ZohoAuthResult.Success(userId = userId, name = name)
                    } else {
                        ZohoAuthResult.Error("Authentication succeeded but no user_id was returned.")
                    }
                } else {
                    val message = extractErrorMessage(parsed)
                        ?: if (!response.isSuccessful) {
                            "Authentication failed (HTTP ${response.code})."
                        } else {
                            "Account not authorized. Please verify your email address."
                        }
                    ZohoAuthResult.Error(message)
                }
            }
        } catch (e: IOException) {
            ZohoAuthResult.Error("Network error: ${e.localizedMessage ?: "Unable to reach Zoho server."}")
        } catch (e: Exception) {
            ZohoAuthResult.Error("Invalid server response: ${e.localizedMessage ?: "Parse error."}")
        }
    }

    suspend fun postLocation(
        type: String,
        userId: String,
        latitude: Double,
        longitude: Double,
    ): ZohoPostResult = withContext(Dispatchers.IO) {
        val coordsJson = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
        }
        val payload = JSONObject().apply {
            put("type", type)
            put("user_id", userId)
            put("data", coordsJson)
        }

        val request = Request.Builder()
            .url(POST_API_URL)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (bodyText.isBlank()) {
                    return@withContext if (response.isSuccessful) {
                        ZohoPostResult.Success("Uploaded $type fix")
                    } else {
                        ZohoPostResult.NetworkError("Server returned HTTP ${response.code}")
                    }
                }

                val parsed = parseZohoPayload(bodyText)
                val status = parsed.optString("status", "")

                when {
                    status.equals("error", ignoreCase = true) -> {
                        val message = extractErrorMessage(parsed)
                            ?: "Zoho API rejected location sync."
                        ZohoPostResult.ZohoApiError(message)
                    }
                    status.equals("success", ignoreCase = true) || response.isSuccessful -> {
                        val message = parsed.optString("message")
                            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                            ?: "Synced $type coordinates"
                        ZohoPostResult.Success(message)
                    }
                    else -> {
                        val message = extractErrorMessage(parsed)
                            ?: "Sync error (HTTP ${response.code})"
                        ZohoPostResult.NetworkError(message)
                    }
                }
            }
        } catch (e: IOException) {
            ZohoPostResult.NetworkError(e.localizedMessage ?: "Network unavailable")
        } catch (e: Exception) {
            ZohoPostResult.NetworkError(e.localizedMessage ?: "Response parse failure")
        }
    }

    private fun extractErrorMessage(json: JSONObject): String? {
        val candidates = listOf("message", "error", "description", "detail")
        for (key in candidates) {
            val value = json.optString(key, "").trim()
            if (value.isNotEmpty() && !value.equals("null", ignoreCase = true)) {
                return value
            }
        }
        return null
    }
}
