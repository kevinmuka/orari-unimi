package app.orariunimi

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterTest {
    @Test
    fun comparesSemanticVersionsNumerically() {
        assertTrue(AppUpdater.isNewer("1.4.0", "1.3.1"))
        assertTrue(AppUpdater.isNewer("1.10.0", "1.9.9"))
        assertFalse(AppUpdater.isNewer("1.3.1", "1.3.1"))
        assertFalse(AppUpdater.isNewer("1.3.0", "1.3.1"))
    }

    @Test
    fun acceptsDifferentVersionLengths() {
        assertTrue(AppUpdater.isNewer("2.0.1", "2"))
        assertFalse(AppUpdater.isNewer("2.0", "2.0.0"))
        assertFalse(AppUpdater.isNewer("not-a-version", "1.0.0"))
    }

    @Test
    fun prefersVersionCodeWhenReleaseProvidesIt() {
        val sameVersionNewBuild = AppRelease("1.6.6", "android-v1.6.6", "https://example.test/app.apk", null, 24)
        assertTrue(AppUpdater.isNewer(sameVersionNewBuild, "1.6.6", 23))
        assertFalse(AppUpdater.isNewer(sameVersionNewBuild, "1.6.6", 24))
    }

    @Test
    fun parsesVersionCodeFromCanonicalMarkerOrHumanText() {
        assertTrue(AppUpdater.parseReleaseVersionCode(
            "<!-- orari-unimi-version-code: 24 -->") == 24)
        assertTrue(AppUpdater.parseReleaseVersionCode("Build Android con versionCode `25`") == 25)
        assertTrue(AppUpdater.parseReleaseVersionCode("Nessun codice build") == null)
    }

    @Test
    fun selectsInAppOnlyReleaseOnlyWhenEnabled() {
        fun release(tag: String, code: Int, inAppOnly: Boolean) = JSONObject().apply {
            put("tag_name", tag)
            put("body", "`versionCode $code`" + if (inAppOnly)
                "\n<!-- orari-unimi-update: in-app-only -->" else "")
            put("assets", JSONArray().put(JSONObject().apply {
                put("name", "orari-unimi.apk")
                put("browser_download_url",
                    "https://github.com/kevinmuka/orari-unimi/releases/download/$tag/orari-unimi.apk")
            }))
        }
        val response = JSONArray()
            .put(release("android-v1.10.3", 53, false))
            .put(release("v9.0.0", 100, false))
            .put(release("android-v1.10.4", 54, true))
            .toString()

        val automatic = AppUpdater.latestAndroidRelease(response, includeInAppOnly = true)
        assertEquals(54, automatic.versionCode)
        assertTrue(automatic.inAppOnly)
        val withoutSilentUpdates = AppUpdater.latestAndroidRelease(response, includeInAppOnly = false)
        assertEquals(53, withoutSilentUpdates.versionCode)
        assertFalse(withoutSilentUpdates.inAppOnly)
    }

    @Test
    fun silentReleaseNeverPostsToAndroidButNormalReleaseCan() {
        val normal = AppRelease("1.10.3", "android-v1.10.3", "https://example.test/app.apk", null)
        val silent = normal.copy(inAppOnly = true)
        assertTrue(AppUpdater.shouldPostSystemUpdate(normal, allowSystemNotification = true))
        assertFalse(AppUpdater.shouldPostSystemUpdate(silent, allowSystemNotification = true))
        assertFalse(AppUpdater.shouldPostSystemUpdate(normal, allowSystemNotification = false))
        assertTrue(AppUpdater.isInAppOnly("<!-- ORARI-UNIMI-UPDATE: IN-APP-ONLY -->"))
        assertFalse(AppUpdater.isInAppOnly("Aggiornamento silenzioso"))
    }

    @Test
    fun showsOnlyReaderFacingReleaseSectionsAndUnwrapsMarkdown() {
        val body = """
            # Android 1.10.0
            `versionCode 47`
            <!-- orari-unimi-update: in-app-only -->

            ## Aule
            - In **Esplora → Aule** si vedono gli edifici e
              i relativi spazi. Leggi [i dettagli](https://example.test/docs).

            ## Calendari
            - Il widget torna a oggi.

            Verifiche: build firmata e checksum controllato.
        """.trimIndent()
        assertEquals(listOf(
            ReleaseNoteSection("Aule", listOf(
                "In Esplora → Aule si vedono gli edifici e i relativi spazi. Leggi i dettagli.")),
            ReleaseNoteSection("Calendari", listOf("Il widget torna a oggi."))
        ), releaseNoteSections(body))
    }
}
