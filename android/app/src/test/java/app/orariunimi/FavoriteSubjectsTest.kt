package app.orariunimi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteSubjectsTest {
    @Test fun combinesLessonAndExamSelectionsWithoutRepeatingTheSubject() {
        val source = ExamSubjectSource("FBA", "FBA-12")
        val subjects = favoriteSubjects(
            lessons = listOf(SavedSubject("2026", "ABC", "Algoritmi")),
            examOnly = listOf(FollowedExamSubject("2026", "ABC", "Algoritmi", listOf(source))),
            currentYear = "2026",
            indexedSubjects = listOf(SearchItem("ABC", "Algoritmi", SearchKind.SUBJECT,
                examSources = listOf(source)))
        )

        assertEquals(1, subjects.size)
        assertTrue(subjects.single().inLessons)
        assertTrue(subjects.single().inExams)
        assertTrue(subjects.single().manuallyFollowedForExams)
        assertEquals(listOf(source), subjects.single().examSources)
    }

    @Test fun keepsExamOnlySubjectsAndDoesNotBorrowSourcesFromAnotherYear() {
        val subjects = favoriteSubjects(
            lessons = listOf(SavedSubject("2025", "ABC", "Analisi")),
            examOnly = listOf(FollowedExamSubject("2026", "XYZ", "Fisica",
                listOf(ExamSubjectSource("FAA", "FAA-10")))),
            currentYear = "2026",
            indexedSubjects = listOf(SearchItem("ABC", "Analisi", SearchKind.SUBJECT,
                examSources = listOf(ExamSubjectSource("NEW", "NEW-01"))))
        )

        assertEquals(2, subjects.size)
        val oldLesson = subjects.single { it.code == "ABC" }
        assertTrue(oldLesson.inLessons)
        assertFalse(oldLesson.inExams)
        assertTrue(oldLesson.examSources.isEmpty())
        val examOnly = subjects.single { it.code == "XYZ" }
        assertFalse(examOnly.inLessons)
        assertTrue(examOnly.inExams)
        assertEquals(1, examOnly.examSources.size)
    }

    @Test fun aSavedLessonCanOptOutOfExamFollowingWithoutLosingItsExamSource() {
        val source = ExamSubjectSource("FBA", "FBA-12")
        val selected = favoriteSubjects(
            lessons = listOf(SavedSubject("2026", "ABC", "Algoritmi")),
            examOnly = emptyList(),
            currentYear = "2026",
            indexedSubjects = listOf(SearchItem("ABC", "Algoritmi", SearchKind.SUBJECT,
                examSources = listOf(source))),
            excludedFromExams = setOf("2026" to "ABC")
        ).single()

        assertTrue(selected.inLessons)
        assertFalse(selected.inExams)
        assertEquals(listOf(source), selected.examSources)
    }
}
