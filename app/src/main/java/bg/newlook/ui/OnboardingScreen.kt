package bg.newlook.ui

import android.animation.ValueAnimator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import bg.newlook.R
import com.airbnb.lottie.compose.*

@Composable
fun BrandSplashContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Image(painterResource(R.drawable.newlook_logo), null, Modifier.size(140.dp))
        Text("NEW LOOK", style = MaterialTheme.typography.headlineSmall)
        Text(
            l("Повече яснота. Всеки ден.", "A little clearer. Every day."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(Modifier.width(96.dp))
    }
}

@Composable
fun Onboarding(preview: Boolean = false, done: (Boolean) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Box(if (preview) Modifier.fillMaxSize() else Modifier.fillMaxSize().safeDrawingPadding()) {
        Page {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrandWordmark()
                Spacer(Modifier.weight(1f))
                if (page < 2) TextButton(onClick = { page = 2 }) { Text(l("Пропусни", "Skip")) }
            }
            Crossfade(page, animationSpec = tween(350), label = "onboarding page") { current ->
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f),
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(230.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val composition by
                                rememberLottieComposition(
                                    LottieCompositionSpec.Asset(
                                        "animations/onboarding_$current.json"
                                    )
                                )
                            val motion = ValueAnimator.areAnimatorsEnabled()
                            val progress by
                                animateLottieCompositionAsState(
                                    composition,
                                    iterations = 3,
                                    isPlaying = motion,
                                )
                            LottieAnimation(
                                composition,
                                progress = { if (motion) progress else .65f },
                                modifier = Modifier.size(210.dp),
                            )
                        }
                    }
                    Text(
                        when (current) {
                            0 -> l("Една цел.\nЯсен план.", "One goal.\nA clear plan.")
                            1 ->
                                l("Място за\nважните срещи.", "Make room for\nmeaningful meetings.")
                            else ->
                                l("Вашият ритъм.\nВашите шаблони.", "Your rhythm.\nYour templates.")
                        },
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Muted(
                        when (current) {
                            0 ->
                                l(
                                    "Съберете задачите в проекти, движете ги по етапи и виждайте напредъка. Завършеното остава в историята.",
                                    "Bring tasks into projects, move them through stages and see your progress. Completed work stays in history.",
                                )
                            1 ->
                                l(
                                    "Планирайте нова среща или отворете календара със създадените срещи. Всичко важно е подръка.",
                                    "Schedule a new meeting or open your booked dates. Keep every important detail close at hand.",
                                )
                            else ->
                                l(
                                    "Създайте свои чеклисти или започнете с готов шаблон. Изберете проект и начална дата — задачите са готови.",
                                    "Build your own checklists or use a ready-made template. Choose a project and a start date — your tasks are ready.",
                                )
                        }
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(3) { index ->
                    Box(
                        Modifier.padding(horizontal = 4.dp)
                            .width(if (page == index) 26.dp else 7.dp)
                            .height(7.dp)
                            .background(
                                if (page == index) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            )
                    )
                }
            }
            if (page < 2) {
                Button(
                    onClick = { page++ },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                ) {
                    Text(l("Продължи", "Continue"))
                }
            } else {
                Button(
                    onClick = { done(false) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                ) {
                    Text(
                        if (preview) l("Към приложението", "Back to app")
                        else l("Започнете на чисто", "Start fresh")
                    )
                }
                if (!preview)
                    OutlinedButton(onClick = { done(true) }, modifier = Modifier.fillMaxWidth()) {
                        Text(l("Разгледайте демо пространството", "Explore the demo workspace"))
                    }
                Muted(
                    l(
                        "Лично пространство. Данните остават на устройството.",
                        "Your personal space. Your data stays on this device.",
                    )
                )
            }
            if (page > 0) TextButton(onClick = { page-- }) { Text(l("Назад", "Back")) }
        }
    }
}
