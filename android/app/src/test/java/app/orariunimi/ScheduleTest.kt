package app.orariunimi

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.DayOfWeek

class ScheduleTest {
    @Test fun portalDegreeLabelsHaveDistinctTypes() {
        assertEquals(DegreeType.BACHELOR, DegreeType.fromPortal("CDS TRIENNALE"))
        assertEquals(DegreeType.MASTER, DegreeType.fromPortal("CDS MAGISTRALE"))
        assertEquals(DegreeType.SINGLE_CYCLE, DegreeType.fromPortal("CDS MAGISTRALE A CICLO UNICO"))
        assertEquals(DegreeType.OTHER, DegreeType.fromPortal("master"))
    }

    @Test fun searchIgnoresAccentsAndMatchesCode() {
        val items = listOf(
            SearchItem("MAT01", "Matematica", SearchKind.SUBJECT),
            SearchItem("FIS02", "Fisica e società", SearchKind.SUBJECT)
        )
        assertEquals(listOf(items[1]), filterItems(items, "societa"))
        assertEquals(listOf(items[0]), filterItems(items, "mat01"))
        assertEquals(listOf(items[1]), SearchIndex(items).search("societa"))
    }

    @Test fun emptyCourseUsesTeachingsFromMatchingCatalogCode() {
        val course = SearchItem("FBA", "INFORMATICA", SearchKind.COURSE,
            paths = listOf("FBA-0|insegnamenticomplementari"),
            coursePaths = listOf(CoursePath("FBA-0|insegnamenticomplementari", "Unico", emptyList())))
        val candidates = listOf(
            CourseTeachingCandidate("FBA^FBA-0^FBA-91", CourseTeaching("ECFBA-91_1", "Affective Computing", "")),
            CourseTeachingCandidate("FAA^FAA-0^FAA-91", CourseTeaching("ECFAA-91_1", "Altro", ""))
        )
        val completed = course.withFallbackTeachings(candidates)
        assertEquals(1, completed.coursePaths.single().teachings.size)
        assertEquals("ECFBA-91_1", completed.coursePaths.single().teachings.single().code)
        assertEquals(course.paths, completed.paths)
    }

    @Test fun fallbackKeepsAnUnambiguousCoursePathName() {
        val course = SearchItem("FBA", "INFORMATICA", SearchKind.COURSE,
            coursePaths = listOf(CoursePath("FBA-0|insegnamenticomplementari", "Unico", emptyList())))
        val completed = course.withFallbackTeachings(listOf(
            CourseTeachingCandidate("FBA^FBA-0^FBA-91", CourseTeaching("ECFBA-91_1", "Affective Computing", ""))
        ))
        assertEquals("Unico", completed.coursePaths.single().name)
    }

    @Test fun monthGridStartsOnMondayAndUsesOnlyNeededWeeks() {
        val dates = lessonMonthDates(YearMonth.of(2026, 9))
        assertEquals(35, dates.size)
        assertEquals(LocalDate.of(2026, 8, 31), dates.first())
        assertEquals(LocalDate.of(2026, 9, 1), dates[1])
        assertEquals(LocalDate.of(2026, 9, 30), dates[30])
        assertEquals(LocalDate.of(2026, 10, 4), dates.last())
    }

    @Test fun lessonMonthGridHidesWeekendsWhenDisabled() {
        val dates = lessonMonthDates(YearMonth.of(2026, 9), weekend = false)
        assertEquals(25, dates.size)
        assertEquals(true, dates.all { it.dayOfWeek.value <= DayOfWeek.FRIDAY.value })
    }

    @Test fun combinedSubjectCodeLinksSchedulesToExamEndpoint() {
        assertEquals(ExamSubjectSource("FBA", "FBA-91"), examSubjectSource("FBA^FBA-0^FBA-91"))
        assertEquals(null, examSubjectSource("broken"))
    }

    @Test fun subjectCatalogMergesExamSourcesForTheSameTeaching() {
        val body = """[
            {"valore":"ECFBA-91_1","label":"Affective Computing","codice_combinato":"FBA^FBA-0^FBA-91"},
            {"valore":"ECFBA-91_1","label":"Affective Computing","codice_combinato":"FAA^FAA-0^FAA-91"}
        ]""".trimIndent()
        val subjects = UnimiApi(transport = { _, _, _ -> "var elenco_attivita = $body;" })
            .entries(SearchKind.SUBJECT, "2026")
        assertEquals(1, subjects.size)
        assertEquals(listOf(
            ExamSubjectSource("FBA", "FBA-91"),
            ExamSubjectSource("FAA", "FAA-91")
        ), subjects.single().examSources)
    }

    @Test fun followedSubjectOnlyKeepsItsAppealsFromEachCourse() {
        val subject = FollowedExamSubject("2026", "ECFBA-91_1", "Affective Computing",
            listOf(ExamSubjectSource("FBA", "FBA-91")))
        val matching = ExamAppeal("1", "FBA", "Informatica", "FBA-91", "", "Affective Computing",
            LocalDate.of(2026, 9, 22), "09:00", "", "", "", "", "", "", null, null)
        val other = matching.copy(id = "2", subjectCode = "FBA-92", subjectName = "Altro")
        val merged = mergeSubjectExamSnapshots(subject, listOf(
            ExamSubjectSource("FBA", "FBA-91") to ExamSnapshot(listOf(matching, other), 100L)))
        assertEquals(listOf(matching), merged?.appeals)
    }

    @Test fun responseCacheExpiresAndPrunesOldEntries() {
        val directory = Files.createTempDirectory("orari-cache-test").toFile()
        var now = 1_000L
        try {
            val cache = ResponseCache(directory, maxBytes = 10, clockMillis = { now })
            cache.write("first", "123456")
            assertEquals("123456", cache.read("first", 100))
            assertEquals(1_000L, cache.readEntry("first", 100)?.storedAtMillis)
            now = 1_200L
            assertEquals(null, cache.read("first", 100))
            assertEquals("123456", cache.read("first", 300))
            now = 2_000L
            cache.write("second", "abcdef")
            assertEquals(null, cache.read("first", 10_000))
            assertEquals("abcdef", cache.read("second", 100))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun calendarOpensOnTodayAndKeepsSelectionVisibleWithoutWeekend() {
        val tuesday = LocalDate.of(2026, 9, 15)
        val saturday = LocalDate.of(2026, 9, 19)
        assertEquals(tuesday, openingDay(tuesday, false))
        assertEquals(saturday, openingDay(saturday, true))
        assertEquals(LocalDate.of(2026, 9, 18), openingDay(saturday, false))

        val lesson = Lesson("1", "ABC", "Lezione", saturday, "09:00", "11:00", "", "", "", "", false)
        val week = startOfWeek(tuesday)
        assertEquals(week, preferredDay(listOf(lesson), week, false))
        assertEquals(saturday, preferredDay(listOf(lesson), week, true))
    }

    @Test fun adjacentCalendarDaySkipsHiddenWeekend() {
        val friday = LocalDate.of(2026, 9, 18)
        val monday = LocalDate.of(2026, 9, 21)
        assertEquals(monday, adjacentCalendarDay(friday, 1, false))
        assertEquals(friday, adjacentCalendarDay(monday, -1, false))
        assertEquals(LocalDate.of(2026, 9, 19), adjacentCalendarDay(friday, 1, true))
    }

    @Test fun scheduleInsightsFindNextLessonAndOverlaps() {
        val day = LocalDate.of(2026, 9, 16)
        val first = Lesson("1", "A", "Algoritmi", day, "09:00", "11:00", "Aula 1", "", "", "", false)
        val overlap = Lesson("2", "B", "Basi di dati", day, "10:30", "12:30", "Aula 2", "", "", "", false)
        val later = Lesson("3", "C", "Calcolo", day, "14:00", "16:00", "Aula 3", "", "", "", false)
        val cancelled = Lesson("4", "D", "Diritto", day, "08:00", "10:00", "", "", "", "", true)
        val parallelGroup = Lesson("5", "A", "Algoritmi", day, "09:30", "10:30", "Aula 4", "", "", "", false)

        val lessons = listOf(cancelled, later, overlap, parallelGroup, first)
        assertEquals(first, nextLesson(lessons, LocalDateTime.of(2026, 9, 16, 8, 30)))
        assertEquals(first, nextLesson(lessons, LocalDateTime.of(2026, 9, 16, 10, 0)))
        assertEquals(listOf(cancelled, first, parallelGroup, overlap, later), lessonsForDay(lessons, day))
        assertEquals(listOf(LessonConflict(first, overlap)), lessonConflicts(lessons))
        assertEquals(setOf(lessonKey(first), lessonKey(overlap)), conflictingLessonKeys(lessons))
        assertEquals("10:30" to "11:00", conflictInterval(LessonConflict(first, overlap)))
    }

    @Test fun weeklyTimelineReusesLanesAcrossAnOverlapCluster() {
        val day = LocalDate.of(2026, 9, 17)
        val first = Lesson("1", "A", "A", day, "09:00", "10:00", "", "", "", "", false)
        val middle = Lesson("2", "B", "B", day, "09:30", "10:30", "", "", "", "", false)
        val last = Lesson("3", "C", "C", day, "10:00", "11:00", "", "", "", "", false)

        val placements = timelineLessons(listOf(last, middle, first))
        assertEquals(listOf(0, 1, 0), placements.map { it.lane })
        assertEquals(listOf(2, 2, 2), placements.map { it.laneCount })
    }
}
