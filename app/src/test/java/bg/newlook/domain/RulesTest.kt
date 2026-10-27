package bg.newlook.domain

import bg.newlook.data.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun dateOnlyGroupingAndCompletion() {
        val yesterday = Task(title = "Review", due = "2026-10-25")
        assertTrue(overdue(yesterday, clock))
        assertFalse(overdue(completed(yesterday, true, clock), clock))
        assertFalse(overdue(yesterday.copy(due = "2026-10-26"), clock))
        assertFalse(overdue(yesterday.copy(due = null), clock))
        assertEquals(clock.millis(), completed(yesterday, true, clock).completedAt)
        assertNull(completed(completed(yesterday, true, clock), false, clock).completedAt)
    }

    @Test
    fun monthlyPreservesOriginalDayAcrossShortMonths() {
        val s = Series(start = "2024-01-31", frequency = "monthly")
        assertEquals(
            listOf("2024-01-31", "2024-02-29", "2024-03-31", "2024-04-30"),
            dates(s, LocalDate.parse("2024-04-30")).map { it.toString() },
        )
    }

    @Test
    fun nonLeapAndLargeInterval() {
        assertEquals(
            listOf("2025-01-31", "2025-02-28", "2025-03-31"),
            dates(
                    Series(start = "2025-01-31", frequency = "monthly"),
                    LocalDate.parse("2025-03-31"),
                )
                .map { it.toString() },
        )
        assertEquals(
            2,
            dates(Series(start = "2026-01-01", interval = 1000), LocalDate.parse("2029-01-01")).size,
        )
    }

    @Test
    fun weeklyAndEndConditions() {
        val s =
            Series(
                start = "2026-10-26",
                frequency = "weekly",
                weekdays = "1,3",
                interval = 2,
                limit = 3,
            )
        assertEquals(
            listOf("2026-10-26", "2026-10-28", "2026-11-09"),
            dates(s, LocalDate.parse("2027-01-01")).map { it.toString() },
        )
        assertEquals(2, dates(s.copy(until = "2026-11-01"), LocalDate.parse("2027-01-01")).size)
    }

    @Test
    fun recurrenceDatesSurviveDst() {
        val s = Series(start = "2026-10-24", zone = "Europe/Sofia", limit = 3)
        assertEquals(
            listOf("2026-10-24", "2026-10-25", "2026-10-26"),
            dates(s, LocalDate.parse("2026-10-30")).map { it.toString() },
        )
        assertEquals(
            dates(s, LocalDate.parse("2026-10-30")),
            dates(s, LocalDate.parse("2026-10-30")),
        )
    }

    @Test
    fun filtersUseOrWithinAndBetweenGroups() {
        val t =
            Task(title = "Client review", priority = 3, status = "todo", tags = "client, review")
        assertTrue(
            matches(
                t,
                "review",
                Filters(
                    statuses = setOf("todo", "done"),
                    priorities = setOf(2, 3),
                    tags = setOf("review", "other"),
                    hasDue = false,
                ),
            )
        )
        assertFalse(matches(t, "review", Filters(statuses = setOf("done"), priorities = setOf(3))))
        assertFalse(matches(t, "other", Filters()))
    }

    @Test
    fun csvProtectsFormulasAndQuotes() {
        val result = csv(listOf(Task(title = " =SUM(1,2)", description = "a\"b\nc")))
        assertTrue(result.contains("\"' =SUM(1,2)\""))
        assertTrue(result.contains("\"a\"\"b\nc\""))
        assertTrue(result.startsWith("\uFEFF"))
        assertTrue(result.contains("\r\n"))
    }

    @Test
    fun icsExportsAllDayAndUtc() {
        val allDay = Event(title = "A, B", start = "2026-10-26", end = "2026-10-27", allDay = true)
        val text = ics(allDay, clock)
        assertTrue(text.contains("DTEND;VALUE=DATE:20261027"))
        assertTrue(text.contains("SUMMARY:A\\, B"))
        assertTrue(text.contains("UID:${allDay.id}@newlook.local"))
        assertTrue(
            ics(
                    allDay.copy(
                        allDay = false,
                        start = "2026-10-26T23:30:00Z",
                        end = "2026-10-27T00:30:00Z",
                    ),
                    clock,
                )
                .contains("DTSTART:20261026T233000Z")
        )
    }

    @Test
    fun eventSpansMidnight() {
        val e = Event(start = "2026-10-26T23:00:00Z", end = "2026-10-27T02:00:00Z", zone = "UTC")
        assertTrue(eventOn(e, LocalDate.parse("2026-10-27")))
        val all = e.copy(allDay = true, start = "2026-10-26", end = "2026-10-28")
        assertTrue(eventOn(all, LocalDate.parse("2026-10-27")))
        assertFalse(eventOn(all, LocalDate.parse("2026-10-28")))
    }

    @Test
    fun backupRoundtripRetainsIdsAndLinks() {
        val p = Project(title = "P")
        val t = Task(title = "T", projectId = p.id, subtasks = json.toJson(listOf(Subtask("Step"))))
        val b = Backup(tasks = listOf(t), projects = listOf(p))
        val restored = validateBackup(json.toJson(b))
        assertEquals(b, restored)
    }

    @Test
    fun rejectsBrokenReferencesAndDuplicateIds() {
        val t = Task(title = "T", projectId = uuid())
        assertThrows(IllegalArgumentException::class.java) {
            validateBackup(json.toJson(Backup(tasks = listOf(t))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateBackup(
                json.toJson(
                    Backup(tasks = listOf(t.copy(projectId = null), t.copy(projectId = null)))
                )
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateBackup(json.toJson(Backup(schemaVersion = 99)))
        }
    }

    @Test
    fun rejectsBadDatesStatusAndRanges() {
        assertThrows(Exception::class.java) {
            validateBackup(
                json.toJson(Backup(tasks = listOf(Task(title = "T", due = "2026-99-01"))))
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateBackup(json.toJson(Backup(tasks = listOf(Task(title = "T", status = "done")))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateBackup(
                json.toJson(
                    Backup(
                        events =
                            listOf(
                                Event(
                                    title = "T",
                                    allDay = true,
                                    start = "2026-10-27",
                                    end = "2026-10-26",
                                )
                            )
                    )
                )
            )
        }
    }

    @Test
    fun finderChangesWithAnswers() {
        assertEquals(0, recommendations(setOf(0), 0, 0).first().first)
        assertEquals(4, recommendations(setOf(4), 0, 4).first().first)
        assertTrue(recommendations(emptySet(), 0, 5).isEmpty())
    }

    @Test
    fun allTemplatesAreConcreteAndHaveStableIds() {
        assertEquals(8, templates.size)
        assertEquals(8, templates.map { it.id }.toSet().size)
        assertTrue(
            templates.all {
                it.steps.size in 6..12 &&
                    it.steps.all { s -> s.bg.isNotBlank() && s.en.isNotBlank() }
            }
        )
        assertEquals(5, services.size)
        assertEquals(8, guides.size)
    }

    @Test
    fun exactMidnightIsExclusive() {
        val date = LocalDate.parse("2026-10-26")
        val zone = ZoneId.systemDefault()
        val e =
            Event(
                title = "Meeting",
                start = date.atTime(23, 0).atZone(zone).toInstant().toString(),
                end = date.plusDays(1).atStartOfDay(zone).toInstant().toString(),
            )
        assertTrue(eventOn(e, date))
        assertFalse(eventOn(e, date.plusDays(1)))
    }

    @Test
    fun excludedOccurrencesSurviveBackup() {
        val s =
            Series(
                start = "2026-10-26",
                prototype = json.toJson(Task(title = "Recurring", due = "2026-10-26")),
                excludedKeys = "2026-10-27",
            )
        val b = Backup(series = listOf(s))
        assertEquals(s, validateBackup(json.toJson(b)).series.single())
    }

    @Test
    fun timedDeadlinesBecomeOverdueWithinTheDay() {
        val now = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC)
        val task = Task(title = "Timed", due = "2026-10-26", time = "09:30")
        assertTrue(overdue(task, now))
        assertFalse(overdue(task.copy(time = "12:00"), now))
        assertFalse(overdue(task.copy(time = null), now))
        assertFalse(overdue(task.copy(due = "2026-10-27"), now))
        assertFalse(overdue(completed(task, true, now), now))
    }
    @Test fun demoContentFollowsLanguageWithoutTranslatingUserEdits(){
        val demo=Project(title="Демо · Адаптация на служител",description="Демонстрационен проект",demo=true)
        val real=Project(title="Демо · Адаптация на служител",demo=false)
        val custom=Task(title="Моят променен текст",demo=true)
        val task=Task(title="Проверка на CRM",projectId=demo.id,demo=true)
        val w=Workspace(projects=listOf(demo,real),tasks=listOf(task,custom))
        val en=localizedDemo(w,true)
        assertEquals("Demo · Employee onboarding",en.projects[0].title)
        assertEquals(real,en.projects[1])
        assertEquals("CRM review",en.tasks[0].title)
        assertEquals(task.id,en.tasks[0].id)
        assertEquals(task.projectId,en.tasks[0].projectId)
        assertEquals(custom,en.tasks[1])
        assertEquals(w,localizedDemo(en,false))
    }
}
