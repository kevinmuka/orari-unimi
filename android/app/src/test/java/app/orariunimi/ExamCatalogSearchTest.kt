package app.orariunimi

import org.junit.Assert.assertEquals
import org.junit.Test

class ExamCatalogSearchTest {
    @Test
    fun findsCoursesAndAvailableTeachingsWithoutShowingUnlinkedSubjects() {
        val course = SearchItem("F1X", "Informatica", SearchKind.COURSE)
        val linked = SearchItem("ABC", "Informatica teorica", SearchKind.SUBJECT,
            examSources = listOf(ExamSubjectSource("F1X", "ABC")))
        val unlinked = SearchItem("DEF", "Informatica archivio", SearchKind.SUBJECT)

        val results = searchExamCatalogs(SearchIndex(listOf(course)),
            examSubjectSearchIndex(listOf(linked, unlinked)), "informatica")

        assertEquals(listOf(course, linked), results)
        assertEquals(listOf(linked), searchExamCatalogs(null,
            examSubjectSearchIndex(listOf(linked, unlinked)), "ABC"))
    }
}
