package bg.newlook.domain

import bg.newlook.data.Task
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class WeeklyReviewTest {
    @Test
    fun weekBoundariesUseCompletionTimeAndExcludeReopenedTasks() {
        val zone = ZoneId.of("Europe/Sofia")
        val start = LocalDate.parse("2026-10-19")
        fun at(date: String) = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()
        val tasks =
            listOf(
                Task(
                    id = "inside",
                    title = "Done",
                    status = "done",
                    due = "2020-01-01",
                    projectId = "p",
                    completedAt = at("2026-10-19"),
                ),
                Task(
                    id = "outside",
                    title = "Next week",
                    status = "done",
                    completedAt = at("2026-10-26"),
                ),
                Task(
                    id = "reopened",
                    title = "Again",
                    status = "todo",
                    completedAt = at("2026-10-20"),
                ),
                Task(id = "unknown", title = "Old", status = "done"),
                Task(id = "remaining", title = "Remaining", due = "2026-10-25"),
                Task(id = "undated", title = "No date"),
            )
        val review = weeklyReview(tasks, start, zone)
        assertEquals(listOf("inside"), review.completed.map { it.id })
        assertEquals(listOf(1, 0, 0, 0, 0, 0, 0), review.dailyCounts)
        assertEquals(mapOf("p" to 1), review.projectCounts)
        assertEquals(listOf("remaining"), review.unfinished.map { it.id })
    }

    @Test
    fun daylightSavingAndSundayStartAreCalendarBased() {
        val zone = ZoneId.of("America/New_York")
        val start = LocalDate.parse("2026-10-26")
        val instant = ZonedDateTime.of(2026, 11, 1, 23, 30, 0, 0, zone).toInstant()
        val review =
            weeklyReview(
                listOf(
                    Task(
                        title = "DST completion",
                        status = "done",
                        completedAt = instant.toEpochMilli(),
                    )
                ),
                start,
                zone,
            )
        assertEquals(1, review.completed.size)
        assertEquals(1, review.dailyCounts.last())
        assertEquals(LocalDate.parse("2026-10-25"), weekStart(LocalDate.parse("2026-10-27"), 7))
        assertEquals(LocalDate.parse("2026-10-26"), weekStart(LocalDate.parse("2026-10-27"), 1))
    }
}
