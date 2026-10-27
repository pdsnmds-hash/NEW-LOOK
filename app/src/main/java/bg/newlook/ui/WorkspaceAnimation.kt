package bg.newlook.ui

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

@Composable
fun WorkspaceAnimation(success: Boolean, modifier: Modifier = Modifier) {
    val composition by
        rememberLottieComposition(
            LottieCompositionSpec.Asset(
                if (success) "animations/task_success.json" else "animations/empty_list.json"
            )
        )
    val motion = ValueAnimator.areAnimatorsEnabled()
    val progress by
        animateLottieCompositionAsState(
            composition,
            iterations = if (success) 1 else 3,
            isPlaying = motion,
            speed = if (success) .8f else 1f,
        )
    if (composition == null) {
        Icon(
            if (success) AppIcons.CheckCircle else AppIcons.Checklist,
            null,
            modifier = modifier.clearAndSetSemantics {},
            tint = MaterialTheme.colorScheme.secondary,
        )
        return
    }
    LottieAnimation(
        composition,
        progress = { if (motion) progress else .75f },
        modifier = modifier.clearAndSetSemantics {},
    )
}

@Composable
fun TaskSuccessNotice() {
    Surface(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkspaceAnimation(true, Modifier.size(68.dp))
            Text(
                l("Задачата е изпълнена!", "Task completed!"),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
