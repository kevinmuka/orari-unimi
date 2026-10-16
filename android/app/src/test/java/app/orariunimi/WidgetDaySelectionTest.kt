package app.orariunimi

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WidgetDaySelectionTest {
    private val today = LocalDate.of(2026, 10, 16)

    @Test fun keepsManualSelectionDuringTheSameDay() {
        assertEquals(today.plusDays(4), widgetDayForToday(today.plusDays(4), today, today))
    }

    @Test fun returnsToTodayAfterTheDateChanges() {
        assertEquals(today, widgetDayForToday(today.plusDays(4), today.minusDays(1), today))
    }

    @Test fun initializesExistingWidgetsOnToday() {
        assertEquals(today, widgetDayForToday(today.plusDays(4), null, today))
        assertEquals(today, widgetDayForToday(null, today, today))
    }
}
