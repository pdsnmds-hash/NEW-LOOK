package bg.newlook

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import bg.newlook.data.*
import bg.newlook.ui.NewLook
import java.util.Locale
import kotlinx.coroutines.*

fun localizedContext(base: Context): Context {
    val lang = base.getSharedPreferences("locale", 0).getString("language", "bg")!!
    val config = Configuration(base.resources.configuration)
    config.setLocale(Locale.forLanguageTag(lang))
    return base.createConfigurationContext(config)
}

class NewLookApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(localizedContext(base))
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val repo by lazy { Repository(this, WorkspaceDb.open(this), scope) }

    override fun onCreate() {
        super.onCreate()
        repo
        ReminderScheduler.maintain(this)
    }
}

class MainActivity : ComponentActivity() {
    private var deepLink by mutableStateOf<Intent?>(null)
    private var deepGeneration by mutableLongStateOf(0L)

    override fun onNewIntent(next: Intent) {
        super.onNewIntent(next)
        intent = next
        deepLink = next
        deepGeneration++
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(localizedContext(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener { provider ->
            provider.view
                .animate()
                .alpha(0f)
                .setDuration(220)
                .withEndAction { provider.remove() }
                .start()
        }
        enableEdgeToEdge()
        deepLink = intent
        setContent {
            NewLook(
                (application as NewLookApp).repo,
                deepLink?.getStringExtra("owner"),
                deepLink?.getStringExtra("type"),
                deepGeneration,
                deepLink?.getBooleanExtra("complete", false) ?: false,
            )
        }
    }

    fun language(code: String) {
        getSharedPreferences("locale", 0).edit().putString("language", code).commit()
        if (Build.VERSION.SDK_INT >= 33)
            getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(code)
        else {
            val config = Configuration(application.resources.configuration)
            config.setLocale(Locale.forLanguageTag(code))
            @Suppress("DEPRECATION")
            application.resources.updateConfiguration(config, application.resources.displayMetrics)
        }
        recreate()
    }
}
