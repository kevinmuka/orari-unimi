package app.orariunimi

import android.content.Context
import org.json.JSONObject

enum class SyncDiagnosticKind(val storageKey: String) {
    LESSONS("lessons"), EXAMS("exams")
}

data class SyncDiagnosticState(
    val lastAttemptMillis: Long = 0,
    val lastSuccessMillis: Long = 0,
    val lastError: String? = null,
    val sourceCount: Int = 0,
    val networkRequests: Int = 0,
    val cacheHits: Int = 0,
    val changesFound: Int = 0
)

class SyncDiagnosticsStore(context: Context) {
    private val preferences = context.getSharedPreferences("orari_unimi", Context.MODE_PRIVATE)

    fun read(kind: SyncDiagnosticKind): SyncDiagnosticState = synchronized(lock) {
        val root = readRoot().optJSONObject(kind.storageKey) ?: return@synchronized SyncDiagnosticState()
        SyncDiagnosticState(
            lastAttemptMillis = root.optLong("lastAttemptMillis"),
            lastSuccessMillis = root.optLong("lastSuccessMillis"),
            lastError = root.optString("lastError").takeIf { it.isNotBlank() },
            sourceCount = root.optInt("sourceCount"),
            networkRequests = root.optInt("networkRequests"),
            cacheHits = root.optInt("cacheHits"),
            changesFound = root.optInt("changesFound")
        )
    }

    fun recordAttempt(kind: SyncDiagnosticKind, sourceCount: Int, nowMillis: Long = System.currentTimeMillis()) =
        update(kind) { it.copy(lastAttemptMillis = nowMillis, lastError = null, sourceCount = sourceCount) }

    fun recordSuccess(
        kind: SyncDiagnosticKind,
        sourceCount: Int,
        networkRequests: Int,
        cacheHits: Int,
        changesFound: Int,
        nowMillis: Long = System.currentTimeMillis()
    ) = update(kind) { state ->
        state.copy(lastAttemptMillis = nowMillis, lastSuccessMillis = nowMillis, lastError = null,
            sourceCount = sourceCount, networkRequests = networkRequests, cacheHits = cacheHits,
            changesFound = changesFound)
    }

    fun recordFailure(
        kind: SyncDiagnosticKind,
        sourceCount: Int,
        message: String,
        nowMillis: Long = System.currentTimeMillis()
    ) = update(kind) { state ->
        state.copy(lastAttemptMillis = nowMillis, lastError = message.take(180), sourceCount = sourceCount,
            networkRequests = 0, cacheHits = 0, changesFound = 0)
    }

    private fun update(kind: SyncDiagnosticKind, transform: (SyncDiagnosticState) -> SyncDiagnosticState) =
        synchronized(lock) {
            val root = readRoot()
            val value = transform(read(kind))
            root.put(kind.storageKey, JSONObject()
                .put("lastAttemptMillis", value.lastAttemptMillis)
                .put("lastSuccessMillis", value.lastSuccessMillis)
                .put("lastError", value.lastError.orEmpty())
                .put("sourceCount", value.sourceCount)
                .put("networkRequests", value.networkRequests)
                .put("cacheHits", value.cacheHits)
                .put("changesFound", value.changesFound))
            preferences.edit().putString(KEY, root.toString()).commit()
        }

    private fun readRoot() = try {
        JSONObject(preferences.getString(KEY, "{}") ?: "{}")
    } catch (_: Exception) {
        JSONObject()
    }

    private companion object {
        const val KEY = "sync_diagnostics"
        val lock = Any()
    }
}
