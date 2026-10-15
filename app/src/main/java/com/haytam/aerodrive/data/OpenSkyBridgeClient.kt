package com.haytam.aerodrive.data

import com.haytam.aerodrive.core.Aircraft
import com.haytam.aerodrive.core.OpenSkyDecoder
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class BridgeResult(
    val aircraft: List<Aircraft>,
    val sourceLabel: String,
    val message: String,
    val retryAfterSeconds: Long
)

class BridgeException(message: String, val retryAfterSeconds: Long = 30) : IOException(message)

class OpenSkyBridgeClient {
    // This is only reachable through the local machine or adb reverse.
    private val endpoint = URL("http://127.0.0.1:8765/v1/aircraft")

    fun fetch(): BridgeResult {
        val connection = endpoint.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-AeroDrive-Client", "android-dev")
            val code = connection.responseCode
            val retry = connection.getHeaderField("Retry-After")?.toLongOrNull()?.coerceAtLeast(30) ?: 30
            if (code != 200) {
                val message = when (code) {
                    403 -> "OpenSky access is disabled in the bridge."
                    429 -> "OpenSky rate limit. Waiting before retrying."
                    502, 503, 504 -> "OpenSky is unavailable. Old positions will expire."
                    else -> "Bridge request failed (HTTP $code)."
                }
                throw BridgeException(message, retry)
            }
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 2_000_000) throw BridgeException("Bridge response is too large.")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val body = JSONObject(bytes.toString(Charsets.UTF_8))
            val source = body.getString("source")
            if (source != "opensky" && source != "fixture") {
                throw BridgeException("Unknown aircraft source from the bridge.")
            }
            val rows = when (val states = body.opt("states")) {
                null, JSONObject.NULL -> JSONArray()
                is JSONArray -> states
                else -> throw BridgeException("Invalid aircraft response from the bridge.")
            }
            val aircraft = (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONArray(index) ?: return@mapNotNull null
                val values = (0 until row.length()).map { field ->
                    row.opt(field).takeUnless { it == JSONObject.NULL }
                }
                OpenSkyDecoder.decode(values, simulated = source == "fixture")
            }
            return BridgeResult(
                aircraft,
                if (source == "fixture") "Bridge fixture (simulated)" else "OpenSky Network",
                if (source == "fixture") "Local test data. No OpenSky request was made."
                    else "Reported positions. Updates are not continuous.",
                body.optLong("retry_after_seconds", 30).coerceAtLeast(30)
            )
        } finally {
            connection.disconnect()
        }
    }
}
