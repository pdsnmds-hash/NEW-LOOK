package bg.newlook.ui

import android.content.*
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.room.withTransaction
import bg.newlook.data.*
import bg.newlook.domain.*
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import kotlin.math.ceil

@Composable
fun AssetPhoto(id: String, modifier: Modifier = Modifier) {
    val c = LocalContext.current
    val bitmap by
        produceState<ImageBitmap?>(null, id) {
            value =
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                            c.assets.open("images/$id.webp").use {
                                BitmapFactory.decodeStream(it).asImageBitmap()
                            }
                        }
                        .getOrNull()
                }
        }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        bitmap?.let {
            Image(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
fun Photo(id: String) {
    AssetPhoto(id, Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(26.dp)))
}

@Composable
fun ContentCard(title: String, description: String, image: String, open: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            .clickable(onClick = open)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetPhoto(image, Modifier.size(88.dp).clip(RoundedCornerShape(16.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            AppIcons.ArrowForward,
            null,
            Modifier.size(16.dp).rotate(-45f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun FavoriteButton(type: String, id: String, w: Workspace, repo: Repository, vm: WorkspaceModel) {
    val selected = Favorite(type, id) in w.favorites
    TextButton(onClick = { vm.run({ repo.favorite(type, id) }) }) {
        Icon(
            if (selected) AppIcons.Bookmark else AppIcons.BookmarkBorder,
            null,
            Modifier.size(19.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(if (selected) l("В избрани", "Saved") else l("Запази за по-късно", "Save for later"))
    }
}

@Composable
fun categoryLabel(index: Int) =
    when (index) {
        0 -> l("Организация", "Organization")
        1 -> l("Клиенти", "Customers")
        2 -> l("Екип", "People")
        3 -> l("Данни", "Data")
        else -> l("Финанси", "Finance")
    }

@Composable
fun CompanyScreen(w: Workspace, repo: Repository, vm: WorkspaceModel, navigate: (String) -> Unit) {
    val en = english()
    Page {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandWordmark()
            Spacer(Modifier.weight(1f))
            ActionIcon(AppIcons.AlternateEmail, l("Контакти", "Contact")) { navigate("contacts") }
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 310.dp).clip(RoundedCornerShape(30.dp))) {
            AssetPhoto("home", Modifier.matchParentSize())
            Box(
                Modifier.matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF11182A).copy(alpha = .05f),
                                Color(0xFF11182A).copy(alpha = .82f),
                            )
                        )
                    )
            )
            Column(
                Modifier.align(Alignment.BottomStart).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "BUSINESS SUPPORT / SOFIA",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.4.sp,
                    color = Color(0xFFD9E7DE),
                )
                Text(
                    l("По-ясна работа.\nПовече възможности.", "Clearer work.\nMore possibility."),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(Color(0xFFBCE7D1), CircleShape))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "NEW LOOK 1 LTD EOOD",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = .8f),
                    )
                }
            }
        }
        Muted(
            l(
                "Подреждаме ежедневните процеси, за да имате повече време за хората, клиентите и развитието.",
                "We help organize everyday operations, making more room for people, customers and growth.",
            )
        )
        Button(
            onClick = { navigate("finder") },
            Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(17.dp),
        ) {
            Text(l("Открийте вашата подкрепа", "Find your kind of support"))
            Spacer(Modifier.weight(1f))
            Icon(AppIcons.ArrowForward, null, Modifier.size(19.dp))
        }
        Section(l("Как можем да помогнем", "A little support goes a long way"))
        services.forEachIndexed { index, service ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "0${index + 1} / " + categoryLabel(index),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ContentCard(service.title.get(en), service.description.get(en), service.image) {
                    navigate("service/${service.id}")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        PageHeader(
            l("По-близо до\nвашата работа.", "Closer to\nthe way you work."),
            l("ЗА NEW LOOK", "ABOUT NEW LOOK"),
        )
        Photo("about")
        Muted(
            l(
                "NEW LOOK 1 LTD EOOD подкрепя стартиращи, малки и средни предприятия и развити организации в подреждането на административни и оперативни процеси. Работим за по-ясни задачи и повече време за клиенти и развитие.",
                "NEW LOOK 1 LTD EOOD supports startups, small and medium businesses and established organizations in organizing administrative and operational processes. The aim is clearer work and more time for customers and growth.",
            )
        )
        Section(l("От разговор към действие", "From conversation to action"))
        val steps =
            listOf(
                l("Вашите нужди", "Your needs"),
                l("Ясен обхват", "A clear scope"),
                l("Подреден процес", "An organized process"),
                l("Текуща подкрепа", "Ongoing support"),
            )
        steps.forEachIndexed { i, title ->
            var expanded by rememberSaveable { mutableStateOf(i == 0) }
            CardBlock({ expanded = !expanded }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "0${i + 1}",
                        Modifier.width(36.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Icon(
                        if (expanded) AppIcons.ExpandMore else AppIcons.Add,
                        null,
                        Modifier.size(18.dp),
                    )
                }
                if (expanded) {
                    Photo("process_${i + 1}")
                    Muted(
                        when (i) {
                            0 ->
                                l(
                                    "Уточняваме текущите затруднения, обхвата и желания резултат.",
                                    "We clarify current friction, scope and the intended result.",
                                )
                            1 ->
                                l(
                                    "Съгласуваме конкретни дейности, отговорности и очаквания.",
                                    "We agree concrete activities, responsibilities and expectations.",
                                )
                            2 ->
                                l(
                                    "Подреждаме информацията и последователността на работата.",
                                    "We structure information and the order of work.",
                                )
                            else ->
                                l(
                                    "Преглеждаме резултатите и адаптираме административната подкрепа.",
                                    "We review results and adapt administrative support.",
                                )
                        }
                    )
                }
            }
        }
        Section(l("Полезно още днес", "Useful from day one"), l("Справочник", "Guides")) {
            navigate("guides")
        }
        guides.take(2).forEach { g ->
            ContentCard(
                g.title.get(en),
                "${readingMinutes(g.body.get(en))} " + l("мин четене", "min read"),
                g.image,
            ) {
                navigate("article/${g.id}")
            }
        }
        ContactBanner { navigate("contacts") }
        Text(
            l(
                "Снимките са илюстративни и не представят служители на компанията.",
                "Images are illustrative and do not identify company employees.",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ContactBanner(click: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = click),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.secondary)
            Text(
                l("Да освободим време\nза вашия бизнес.", "Let’s make room\nfor your business."),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Row {
                Text(
                    l("Нека поговорим", "Let’s talk"),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Icon(
                    AppIcons.ArrowForward,
                    null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
fun ServiceScreen(
    s: Service,
    w: Workspace,
    repo: Repository,
    vm: WorkspaceModel,
    back: () -> Unit,
    navigate: (String) -> Unit,
) {
    val en = english()
    val index = services.indexOf(s)
    Page {
        BackTitle(s.title.get(en), back)
        Tag(categoryLabel(index))
        Photo(s.image)
        FavoriteButton("service", s.id, w, repo, vm)
        Text(s.description.get(en), style = MaterialTheme.typography.bodyLarge)
        Section(l("Кога е полезно", "When it helps"))
        Muted(
            l(
                "Когато ежедневната координация отнема време от основната ви работа, започнете с конкретен процес и ясни отговорности. Обхватът се уточнява спрямо действителните нужди на организацията.",
                "When daily coordination takes time away from core work, start with a concrete process and clear responsibilities. Scope is agreed around the actual needs of your organization.",
            )
        )
        Section(l("Започнете сами", "Make a start yourself"))
        templates
            .filter { services[it.service].id == s.id }
            .forEach { t -> WorkflowCard(t, en) { navigate("template/${t.id}") } }
        ContactBanner { navigate("contacts") }
    }
}

fun readingMinutes(body: String) =
    ceil(body.split(Regex("\\s+")).size / 180.0).toInt().coerceAtLeast(1)

@Composable
fun GuidesScreen(
    query: String,
    change: (String) -> Unit,
    back: () -> Unit,
    navigate: (String) -> Unit,
) {
    val en = english()
    Page {
        BackTitle(l("Полезно знание.", "Useful by design."), back)
        Muted(
            l(
                "Практически насоки за по-подредена работа.",
                "Practical reading for a more organized working day.",
            )
        )
        SearchField(query, change)
        guides
            .filter { (it.title.get(en) + it.body.get(en)).contains(query, true) }
            .forEach { g ->
                ContentCard(
                    g.title.get(en),
                    "${readingMinutes(g.body.get(en))} " +
                        l("мин четене", "min read") +
                        " · " +
                        categoryLabel(g.service),
                    g.image,
                ) {
                    navigate("article/${g.id}")
                }
            }
    }
}

@Composable
fun ArticleScreen(
    g: Guide,
    w: Workspace,
    repo: Repository,
    vm: WorkspaceModel,
    back: () -> Unit,
    navigate: (String) -> Unit,
) {
    val en = english()
    Page {
        BackTitle(g.title.get(en), back)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(categoryLabel(g.service))
            Text(
                "${readingMinutes(g.body.get(en))} " + l("мин четене", "min read"),
                Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Photo(g.image)
        FavoriteButton("article", g.id, w, repo, vm)
        g.body.get(en).split("\n\n").forEachIndexed { index, paragraph ->
            val parts = paragraph.split('\n', limit = 2)
            if (parts.size == 2 && parts[0].length < 70) {
                Text(
                    parts[0],
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(parts[1], style = MaterialTheme.typography.bodyLarge)
            } else
                Text(
                    paragraph,
                    style =
                        if (index == 0) MaterialTheme.typography.titleLarge
                        else MaterialTheme.typography.bodyLarge,
                    color =
                        if (index == 0) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
        Button(
            onClick = { navigate("template/${g.templateId}") },
            Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(17.dp),
        ) {
            Text(l("Превърнете прочетеното в план", "Turn this into a plan"))
        }
    }
}

@Composable
fun WorkflowCard(t: Workflow, en: Boolean, open: () -> Unit) {
    CardBlock(open) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(AppIcons.AutoAwesome)
            Spacer(Modifier.weight(1f))
            Tag("${t.steps.size} " + l("стъпки", "steps"))
        }
        Text(t.title.get(en), style = MaterialTheme.typography.titleLarge)
        Muted(t.result.get(en))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                categoryLabel(t.service),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Icon(
                AppIcons.ArrowForward,
                null,
                Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun TemplateList(
    w: Workspace,
    q: String,
    change: (String) -> Unit,
    create: () -> Unit,
    open: (String) -> Unit,
) {
    val en = english()
    var category by rememberSaveable { mutableIntStateOf(-1) }
    Page {
        PageHeader(
            l("Добър план.\nПо-лесен старт.", "A good plan.\nA simpler start."),
            l("БИБЛИОТЕКА ОТ ШАБЛОНИ", "WORKFLOW LIBRARY"),
            l("От идея към първа стъпка — за минута.", "From idea to first step, in a minute."),
        )
        Button(onClick = create, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Icon(AppIcons.Add, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(l("Нов собствен шаблон", "Create your own template"))
        }
        if (w.templates.isNotEmpty()) {
            Section(l("Създадени от вас", "Made by you"))
            w.templates
                .filter { it.title.contains(q, true) }
                .forEach { t -> MenuCard(t.title, AppIcons.Checklist) { open(t.id) } }
        }
        SearchField(q, change)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = category == -1,
                onClick = { category = -1 },
                label = { Text(l("Всички", "All")) },
                border = null,
            )
            (0..4).forEach { i ->
                FilterChip(
                    selected = category == i,
                    onClick = { category = i },
                    label = { Text(categoryLabel(i)) },
                    border = null,
                )
            }
        }
        templates
            .filter {
                (it.title.get(en) + it.result.get(en)).contains(q, true) &&
                    (category == -1 || category == it.service)
            }
            .forEach { t -> WorkflowCard(t, en) { open(t.id) } }
    }
}

@Composable
fun FinderScreen(navigate: (String) -> Unit) {
    var types by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableIntStateOf(0) }
    var goal by rememberSaveable { mutableIntStateOf(0) }
    var show by rememberSaveable { mutableStateOf(false) }
    val en = english()
    Page {
        Eyebrow(l("ПОДБОР НА УСЛУГИ", "SERVICE FINDER"))
        Heading(l("От какво се нуждае\nвашият бизнес?", "What does your\nbusiness need?"))
        Muted(l("Отговорите се обработват само на устройството.", "Answers stay on this device."))
        MultiChoice(
            (0..4).map { it.toString() },
            types,
            {
                types = it
                show = false
            },
        ) {
            services[it.toInt()].title.get(en)
        }
        Choice(
            l("Колко често?", "How often?"),
            frequency,
            listOf(l("Еднократно", "One time"), l("Седмично", "Weekly"), l("Всеки ден", "Daily")),
        ) {
            frequency = it
            show = false
        }
        Choice(
            l("Основна цел", "Main goal"),
            goal,
            listOf(
                l("По-добра организация", "Better organization"),
                l("По-бързи отговори", "Faster replies"),
                l("Подредена адаптация", "Organized onboarding"),
                l("По-точни данни", "More accurate data"),
                l("Ясни плащания", "Clearer payments"),
                l("Друго", "Something else"),
            ),
        ) {
            goal = it
            show = false
        }
        Button(onClick = { show = true }) { Text(l("Покажи препоръки", "Show recommendations")) }
        if (show) {
            val results =
                recommendations(
                    types.split(',').mapNotNull { it.toIntOrNull() }.toSet(),
                    frequency,
                    goal,
                )
            if (results.isEmpty())
                MenuCard(
                    l("Обща консултация", "General consultation"),
                    AppIcons.ChatBubbleOutline,
                ) {
                    navigate("contacts")
                }
            results.forEach { (i, score) ->
                CardBlock {
                    Text(services[i].title.get(en), fontWeight = FontWeight.SemiBold)
                    Muted(
                        if (i.toString() in types.split(','))
                            l(
                                "Съответства на избран тип задачи.",
                                "Matches a task type you selected.",
                            )
                        else
                            l(
                                "Подкрепя избраната основна цел.",
                                "Supports your selected main goal.",
                            )
                    )
                    TextButton(onClick = { navigate("service/${services[i].id}") }) {
                        Text(l("Разгледайте услугата", "View service"))
                    }
                }
            }
            ContactDraft(
                services
                    .filterIndexed { i, _ -> results.any { it.first == i } }
                    .joinToString { it.title.get(en) }
            )
        }
    }
}

@Composable
fun ContactDraft(subject: String) {
    val c = LocalContext.current
    val en = english()
    var body by
        rememberSaveable(subject) {
            mutableStateOf(
                if (en) "Hello, I would like to discuss: $subject.\n\nMy needs: "
                else "Здравейте, бих искал/а да обсъдим: $subject.\n\nМоите нужди: "
            )
        }
    Field(
        body,
        { body = it },
        l("Преглед и редакция на запитването", "Review and edit your inquiry"),
        true,
    )
    Button(
        onClick = {
            safeIntent(
                c,
                Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse(
                        "mailto:kolevalilia252@gmail.com?subject=" +
                            Uri.encode("NEW LOOK inquiry") +
                            "&body=" +
                            Uri.encode(body)
                    ),
                ),
            )
        }
    ) {
        Text(l("Отвори в пощата", "Open in email"))
    }
    Muted(
        l(
            "Изпращате сами в приложението за поща. Работни записи не са включени.",
            "You send it yourself in your email app. No workspace records are included.",
        )
    )
}

fun safeIntent(c: Context, intent: Intent) {
    try {
        c.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
                c,
                if (c.resources.configuration.locales[0].language == "en")
                    "No app available for this action"
                else "Няма приложение за това действие",
                Toast.LENGTH_LONG,
            )
            .show()
    }
}

@Composable
fun ContactsScreen(back: () -> Unit) {
    val c = LocalContext.current
    Page {
        BackTitle(l("Да поговорим.", "Let’s talk."), back)
        Eyebrow("NEW LOOK 1 LTD EOOD")
        val values =
            listOf(
                "+15012936856",
                "kolevalilia252@gmail.com",
                "33A Ul. Todor Dzhebarov str., Sofia, 1404, Bulgaria",
            )
        values.forEachIndexed { i, v ->
            CardBlock {
                Text(v)
                Row {
                    TextButton(
                        onClick = {
                            safeIntent(
                                c,
                                Intent(
                                    when (i) {
                                        0 -> Intent.ACTION_DIAL
                                        1 -> Intent.ACTION_SENDTO
                                        else -> Intent.ACTION_VIEW
                                    },
                                    Uri.parse(
                                        when (i) {
                                            0 -> "tel:$v"
                                            1 -> "mailto:$v"
                                            else -> "geo:0,0?q=" + Uri.encode(v)
                                        }
                                    ),
                                ),
                            )
                        }
                    ) {
                        Text(
                            when (i) {
                                0 -> l("Обади се", "Call")
                                1 -> l("Имейл", "Email")
                                else -> l("Отвори карта", "Open map")
                            }
                        )
                    }
                    TextButton(
                        onClick = {
                            (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                .setPrimaryClip(ClipData.newPlainText("NEW LOOK", v))
                        }
                    ) {
                        Text(l("Копирай", "Copy"))
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateDetail(
    entryId: String,
    w: Workspace,
    repo: Repository,
    vm: WorkspaceModel,
    back: () -> Unit,
    openService: (String) -> Unit,
    openProject: (String) -> Unit,
) {
    val en = english()
    val builtin = templates.find { it.id == entryId }
    val user = w.templates.find { it.id == entryId }
    if (builtin == null && user == null) {
        Page { BackTitle(l("Шаблонът е премахнат", "Template removed"), back) }
        return
    }
    var title by
        rememberSaveable(entryId) { mutableStateOf(builtin?.title?.get(en) ?: user!!.title) }
    var start by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var editableSteps by
        rememberSaveable(entryId) {
            mutableStateOf(
                user?.steps
                    ?: json.toJson(
                        builtin!!.steps.mapIndexed { i, step -> TemplateStep(step.get(en), i) }
                    )
            )
        }
    var selectedProjectId by rememberSaveable(entryId) { mutableStateOf<String?>(null) }
    val availableProjects = w.projects.filter { !it.archived }
    var createdId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    val steps: List<TemplateStep> =
        json.fromJson(editableSteps, object : TypeToken<List<TemplateStep>>() {}.type)
    Page {
        BackTitle(builtin?.title?.get(en) ?: user!!.title, back)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("${steps.size} " + l("стъпки", "steps"))
            Tag(
                l("Само на устройството", "On your device"),
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.secondaryContainer,
            )
        }
        Text(
            builtin?.result?.get(en) ?: user!!.description,
            style = MaterialTheme.typography.bodyLarge,
        )
        FavoriteButton("template", entryId, w, repo, vm)
        Section(l("Вашето начало", "Your starting point"))
        Choice(
            l("Добави към проект", "Add to project"),
            availableProjects.indexOfFirst { it.id == selectedProjectId } + 1,
            listOf(l("Създай нов проект", "Create a new project")) +
                availableProjects.map { it.title },
        ) { index ->
            selectedProjectId = availableProjects.getOrNull(index - 1)?.id
        }
        if (selectedProjectId == null)
            Field(title, { title = it }, l("Име на новия проект", "New project name"))
        DateField(start, { start = it }, l("Начална дата", "Start date"))
        Button(
            onClick = {
                applying = true
                var id: String? = null
                vm.run(
                    {
                        try {
                            id =
                                repo.apply(
                                    title,
                                    builtin?.result?.get(en) ?: user!!.description,
                                    steps,
                                    LocalDate.parse(start),
                                    projectId = selectedProjectId,
                                )
                        } finally {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                applying = false
                            }
                        }
                    },
                    { createdId = id },
                )
            },
            enabled =
                !applying &&
                    title.isNotBlank() &&
                    steps.all { it.title.isNotBlank() } &&
                    runCatching { LocalDate.parse(start) }.isSuccess,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(17.dp),
        ) {
            if (applying)
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            else {
                Text(l("Създайте вашия план", "Create your plan"))
                Spacer(Modifier.weight(1f))
                Icon(AppIcons.ArrowForward, null, Modifier.size(19.dp))
            }
        }
        createdId?.let { id ->
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        l("Готово. Планът вече е ваш.", "Ready. This plan is now yours."),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    TextButton(onClick = { openProject(id) }) {
                        Text(l("Отвори проекта", "Open project"))
                        Spacer(Modifier.width(8.dp))
                        Icon(AppIcons.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
        Section(l("Какво включва", "What’s inside"))
        steps.forEachIndexed { index, step ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(30.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (user != null)
                        Field(
                            step.title,
                            { value ->
                                editableSteps =
                                    json.toJson(
                                        steps.mapIndexed { i, s ->
                                            if (i == index) s.copy(title = value) else s
                                        }
                                    )
                            },
                            l("Стъпка", "Step") + " ${index + 1}",
                        )
                    else Text(step.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "+${step.offset} · " + l("дни от началото", "days from start"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Muted(
            if (selectedProjectId == null)
                l(
                    "Създава независим проект и редактируеми задачи. Нищо не се изпраща на компанията.",
                    "Creates an independent project and editable tasks. Nothing is sent to the company.",
                )
            else
                l(
                    "Добавя редактируеми задачи към избрания проект. Нищо не се изпраща на компанията.",
                    "Adds editable tasks to the selected project. Nothing is sent to the company.",
                )
        )
        if (user != null) {
            OutlinedButton(
                onClick = {
                    vm.run({
                        require(title.isNotBlank())
                        require(steps.all { it.title.isNotBlank() })
                        repo.dao.template(user.copy(title = title, steps = editableSteps))
                    })
                }
            ) {
                Text(l("Запази промените в шаблона", "Save template changes"))
            }
            TextButton(onClick = { deleting = true }) {
                Text(l("Изтрий шаблона", "Delete template"))
            }
        }
        builtin?.let { t ->
            val service = services[t.service]
            Section(l("Нуждаете се от подкрепа?", "Need a hand?"))
            MenuCard(service.title.get(en), AppIcons.Business) { openService(service.id) }
        }
    }
    if (deleting && user != null)
        Confirm(
            l("Изтриване на шаблон?", "Delete template?"),
            l("Създадените проекти се запазват.", "Previously created projects are kept."),
            { deleting = false },
        ) {
            vm.run(
                {
                    repo.db.withTransaction {
                        repo.dao.deleteFavorite(Favorite("template", user.id))
                        repo.dao.deleteTemplate(user)
                    }
                },
                back,
            )
        }
}
