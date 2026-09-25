package app.orariunimi

import org.junit.Assert.assertFalse
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
}
