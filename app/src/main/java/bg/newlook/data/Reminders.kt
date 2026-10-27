package bg.newlook.data

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import bg.newlook.MainActivity
import bg.newlook.NewLookApp
import bg.newlook.R
import bg.newlook.domain.completed
import java.time.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ReminderScheduler {
    private val mutex = Mutex()

    fun clearSnooze(c: Context, id: String) {
        c.getSharedPreferences("alarms", 0).edit().remove("snooze_" + id).apply()
    }

    private fun pending(
        c: Context,
        key: String,
        owner: String = "",
        title: String = "",
        type: String = "task",
    ) =
        PendingIntent.getBroadcast(
            c,
            0,
            Intent(c, ReminderReceiver::class.java)
                .setData(Uri.parse("newlook://alarm/$key"))
                .putExtra("owner", owner)
                .putExtra("title", title)
                .putExtra("type", type),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    suspend fun reconcile(c: Context, dao: WorkspaceDao) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val alarms = c.getSystemService(AlarmManager::class.java)
                val prefs = c.getSharedPreferences("alarms", 0)
                val previous = prefs.getStringSet("ids", emptySet())!!.toSet()
                previous.forEach { alarms.cancel(pending(c, it)) }
                val ids = mutableSetOf<String>()
                val now = Instant.now()
                fun schedule(id: String, title: String, type: String, at: Instant) {
                    val snooze = prefs.getLong("snooze_" + id, 0L)
                    val effective =
                        if (snooze > now.toEpochMilli()) Instant.ofEpochMilli(snooze) else at
                    if (effective <= now) return
                    if (prefs.getLong("trigger_" + id, 0L) != effective.toEpochMilli())
                        NotificationManagerCompat.from(c).cancel(id.hashCode())
                    alarms.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        effective.toEpochMilli(),
                        pending(c, id, id, title, type),
                    )
                    prefs.edit().putLong("trigger_" + id, effective.toEpochMilli()).apply()
                    ids += id
                }
                val tasks =
                    dao.allTasks().filter {
                        it.status != "done" && it.reminder != null && it.due != null
                    }
                // At most the next future instance of each series gets an alarm.
                val eligible =
                    tasks.filter { it.seriesId == null } +
                        tasks
                            .filter { it.seriesId != null }
                            .groupBy { it.seriesId }
                            .values
                            .mapNotNull { group ->
                                group
                                    .filter {
                                        LocalDate.parse(it.due)
                                            .atTime(
                                                it.time?.let(LocalTime::parse) ?: LocalTime.of(9, 0)
                                            )
                                            .atZone(ZoneId.systemDefault())
                                            .toInstant()
                                            .minusSeconds(it.reminder!! * 60L) > now ||
                                            prefs.getLong("snooze_" + it.id, 0L) >
                                                now.toEpochMilli()
                                    }
                                    .minByOrNull { it.due + it.time }
                            }
                eligible.forEach { t ->
                    schedule(
                        t.id,
                        t.title,
                        "task",
                        LocalDate.parse(t.due)
                            .atTime(t.time?.let(LocalTime::parse) ?: LocalTime.of(9, 0))
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .minusSeconds(t.reminder!! * 60L),
                    )
                }
                dao.allEvents()
                    .filter { it.reminder != null }
                    .forEach { e ->
                        val start =
                            if (e.allDay)
                                LocalDate.parse(e.start)
                                    .atTime(9, 0)
                                    .atZone(ZoneId.of(e.zone))
                                    .toInstant()
                            else Instant.parse(e.start)
                        schedule(e.id, e.title, "event", start.minusSeconds(e.reminder!! * 60L))
                    }
                (previous - ids).forEach {
                    NotificationManagerCompat.from(c).cancel(it.hashCode())
                    prefs.edit().remove("snooze_" + it).remove("trigger_" + it).apply()
                }
                prefs.edit().putStringSet("ids", ids).commit()
            }
        }
    }

    fun maintain(c: Context) {
        WorkManager.getInstance(c)
            .enqueueUniquePeriodicWork(
                "workspace-maintenance",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<MaintenanceWorker>(12, TimeUnit.HOURS).build(),
            )
    }

    suspend fun snoozeAt(c: Context, id: String, title: String, type: String, at: Instant) {
        mutex.withLock {
            require(at > Instant.now())
            val prefs = c.getSharedPreferences("alarms", 0)
            val ids = prefs.getStringSet("ids", emptySet())!!.toSet() + id
            prefs
                .edit()
                .putLong("snooze_" + id, at.toEpochMilli())
                .putStringSet("ids", ids)
                .commit()
            c.getSystemService(AlarmManager::class.java)
                .setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    at.toEpochMilli(),
                    pending(c, id, id, title, type),
                )
            NotificationManagerCompat.from(c).cancel(id.hashCode())
        }
    }

    suspend fun snooze(c: Context, id: String, title: String, type: String, minutes: Int) =
        snoozeAt(c, id, title, type, Instant.now().plusSeconds(minutes * 60L))
}

class MaintenanceWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result =
        try {
            val repo = (applicationContext as NewLookApp).repo
            repo.generate(LocalDate.now().plusMonths(3))
            ReminderScheduler.reconcile(applicationContext, repo.dao)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
}

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return
        val pending = goAsync()
        (c.applicationContext as NewLookApp).scope.launch {
            try {
                val repo = (c.applicationContext as NewLookApp).repo
                repo.generate(LocalDate.now().plusMonths(3))
                ReminderScheduler.reconcile(c, repo.dao)
            } finally {
                pending.finish()
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getStringExtra("owner") ?: return
        val title = i.getStringExtra("title") ?: "NEW LOOK"
        val type = i.getStringExtra("type") ?: "task"
        val app = c.applicationContext as NewLookApp
        if (i.action == "done" || i.action == "snooze10" || i.action == "snooze60") {
            val pending = goAsync()
            app.scope.launch {
                try {
                    if (i.action == "done") {
                        app.repo.dao
                            .allTasks()
                            .find { it.id == id }
                            ?.let { app.repo.save(completed(it, true)) }
                    } else
                        ReminderScheduler.snooze(
                            c,
                            id,
                            title,
                            type,
                            if (i.action == "snooze60") 60 else 10,
                        )
                    NotificationManagerCompat.from(c).cancel(id.hashCode())
                } finally {
                    pending.finish()
                }
            }
            return
        }
        app.scope.launch {
            val exists =
                if (type == "task")
                    app.repo.dao.allTasks().any { it.id == id && it.status != "done" }
                else app.repo.dao.allEvents().any { it.id == id }
            if (!exists) return@launch
            val manager = c.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    "reminders",
                    c.getString(R.string.reminders),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
            val open =
                PendingIntent.getActivity(
                    c,
                    id.hashCode(),
                    Intent(c, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .setData(Uri.parse("newlook://$type/$id"))
                        .putExtra("owner", id)
                        .putExtra("type", type),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val b =
                NotificationCompat.Builder(c, "reminders")
                    .setSmallIcon(R.drawable.brand_icon)
                    .setContentTitle(title)
                    .setContentText(c.getString(R.string.reminders))
                    .setContentIntent(open)
                    .setAutoCancel(true)
            if (type == "task")
                for ((action, label) in
                    listOf(
                        "done" to c.getString(R.string.complete),
                        "snooze10" to "+10 min",
                        "snooze60" to "+1 h",
                    )) {
                    val a =
                        if (action == "done")
                            PendingIntent.getActivity(
                                c,
                                id.hashCode(),
                                Intent(c, MainActivity::class.java)
                                    .addFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    )
                                    .setData(Uri.parse("newlook://complete/$id"))
                                    .putExtra("owner", id)
                                    .putExtra("type", type)
                                    .putExtra("complete", true),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            )
                        else
                            PendingIntent.getBroadcast(
                                c,
                                0,
                                Intent(c, ReminderReceiver::class.java)
                                    .setAction(action)
                                    .setData(Uri.parse("newlook://action/$id/$action"))
                                    .putExtra("owner", id)
                                    .putExtra("title", title)
                                    .putExtra("type", type),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            )
                    b.addAction(0, label, a)
                }
            if (
                Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            )
                manager.notify(id.hashCode(), b.build())
        }
    }
}
