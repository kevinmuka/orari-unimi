package app.orariunimi

import androidx.compose.runtime.saveable.Saver
import java.time.LocalDate
import java.time.YearMonth

internal val LocalDateSaver: Saver<LocalDate, String> = Saver(
    save = { it.toString() }, restore = { LocalDate.parse(it) }
)

internal val YearMonthSaver: Saver<YearMonth, String> = Saver(
    save = { it.toString() }, restore = { YearMonth.parse(it) }
)
