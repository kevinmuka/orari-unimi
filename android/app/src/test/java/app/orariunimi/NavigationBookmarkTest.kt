package app.orariunimi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationBookmarkTest {
    @Test fun restoresAnExamDetailWithItsCourseParentAndSources() {
        val route = NavigationBookmark(
            course = CourseRoute("2026-27", "FBA"),
            exam = ExamRoute("2026-27", "ABC", "Insegnamento", SearchKind.SUBJECT,
                ExamWindow.ALL, fromCourse = false,
                sources = listOf(ExamSubjectSource("FBA", "ABC-1"))),
            appeal = AppealRoute("FBA", "appeal-1", available = true),
            examPlanner = true,
            examPlannerSavedMode = true
        )

        assertEquals(route, NavigationBookmark.decode(route.encode()))
    }

    @Test fun restoresAClassroomWithItsBuildingAndDate() {
        val route = NavigationBookmark(buildingCode = "CEL", roomCode = "A101",
            buildingDay = "2026-10-19", roomDay = "2026-10-20")

        assertEquals(route, NavigationBookmark.decode(route.encode()))
        assertNull(NavigationBookmark.decode("not a bookmark"))
    }

    @Test fun restoresACompoundCalendarWithItsParentAndAgenda() {
        val route = NavigationBookmark(
            course = CourseRoute("2026-27", "FBA"),
            calendar = CalendarRoute("Informatica", "2026-27", SearchKind.COURSE,
                "FBA", "Informatica", combined = false),
            agenda = true,
            subjectSelection = SubjectRoute("2026-27", "ABC", "Insegnamento")
        )

        assertEquals(route, NavigationBookmark.decode(route.encode()))
    }

    @Test fun restoresThePersonalCalendarWithoutASearchSource() {
        val route = NavigationBookmark(calendar = CalendarRoute("Lezioni", "2026-27",
            kind = null, code = "", name = "", combined = true), lessonMonth = true)

        assertEquals(route, NavigationBookmark.decode(route.encode()))
    }
}
