package bg.newlook.domain

import bg.newlook.data.Task
import java.time.*
import java.time.temporal.TemporalAdjusters

data class WeeklyReview(
    val start: LocalDate,
    val completed: List<Task>,
    val dailyCounts: List<Int>,
    val unfinished: List<Task>,
    val projectCounts: Map<String, Int>,
)

fun weekStart(date: LocalDate, firstDay: Int): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(firstDay)))

/** Half-open instant boundaries respect local time and daylight-saving changes. */
fun weeklyReview(tasks: List<Task>, start: LocalDate, zone: ZoneId): WeeklyReview {
    val from = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val until = start.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
    val done =
        tasks
            .filter {
                it.status == "done" &&
                    it.completedAt?.let { time -> time >= from && time < until } == true
            }
            .sortedByDescending { it.completedAt }
    val counts =
        (0L..6L).map { day ->
            done.count {
                Instant.ofEpochMilli(it.completedAt!!).atZone(zone).toLocalDate() ==
                    start.plusDays(day)
            }
        }
    val unfinished =
        tasks
            .filter {
                it.status != "done" &&
                    it.due?.let { due ->
                        val date = LocalDate.parse(due)
                        date >= start && date < start.plusDays(7)
                    } == true
            }
            .sortedBy { it.due }
    return WeeklyReview(
        start,
        done,
        counts,
        unfinished,
        done.mapNotNull { it.projectId }.groupingBy { it }.eachCount(),
    )
}
