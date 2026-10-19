package app.orariunimi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.Job

class NavigationRequestGateTest {
    @Test fun latestNavigationOwnsTheScreen() {
        val gate = NavigationRequestGate()
        val firstCourse = gate.next()
        val firstJob = Job()
        gate.track(firstCourse, firstJob)
        val calendar = gate.next()

        assertTrue(firstJob.isCancelled)
        assertFalse(gate.isCurrent(firstCourse))
        assertTrue(gate.isCurrent(calendar))

        // Pressing Back or changing tab also consumes a generation.
        val calendarJob = Job()
        gate.track(calendar, calendarJob)
        gate.next()
        assertTrue(calendarJob.isCancelled)
        assertFalse(gate.isCurrent(calendar))

        val delayedJob = Job()
        gate.track(calendar, delayedJob)
        assertTrue(delayedJob.isCancelled)
    }
}
