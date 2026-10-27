package bg.newlook.domain

data class LocalText(val bg: String, val en: String) {
    fun get(en: Boolean) = if (en) this.en else bg
}

data class Service(
    val id: String,
    val title: LocalText,
    val description: LocalText,
    val image: String,
)

data class TemplateStep(val title: String, val offset: Int = 0, val priority: Int = 1)

data class Workflow(
    val id: String,
    val title: LocalText,
    val result: LocalText,
    val service: Int,
    val steps: List<LocalText>,
)

data class Guide(
    val id: String,
    val title: LocalText,
    val body: LocalText,
    val templateId: String,
    val service: Int,
    val image: String,
)

val services =
    listOf(
        Service(
            "service_1",
            LocalText("Административна подкрепа", "Administrative & Office Support"),
            LocalText(
                "Календари, срещи, имейл, документи и делова кореспонденция.",
                "Calendars, meetings, email, documents and business correspondence.",
            ),
            "service_1",
        ),
        Service(
            "service_2",
            LocalText("Обслужване на клиенти", "Customer Care & Helpdesk"),
            LocalText(
                "Организация на запитванията и последователна комуникация по имейл, телефон и чат.",
                "Organized requests and consistent communication by email, phone and chat.",
            ),
            "service_2",
        ),
        Service(
            "service_3",
            LocalText("HR и съдействие за заплати", "HR & Payroll Assistance"),
            LocalText(
                "Адаптация, кадрови документи, присъствия и административна координация.",
                "Onboarding, employee records, attendance and administrative coordination.",
            ),
            "service_3",
        ),
        Service(
            "service_4",
            LocalText("Данни и информация", "Data Entry & Information Management"),
            LocalText(
                "Въвеждане, проверка и поддържане на CRM, бази данни и цифрови записи.",
                "Entry, verification and upkeep of CRM, databases and digital records.",
            ),
            "service_4",
        ),
        Service(
            "service_5",
            LocalText("Фактури и администрация", "Billing & Financial Administration"),
            LocalText(
                "Организация на фактури, проследяване на плащания и финансови документи. Не е счетоводен или данъчен съвет.",
                "Organizing invoices, tracking payments and financial paperwork. This is administrative support, not accounting or tax advice.",
            ),
            "service_5",
        ),
    )
val templates =
    listOf(
        Workflow(
            "template_1",
            LocalText("Подготовка на среща", "Meeting preparation"),
            LocalText(
                "Ясна цел, дневен ред и проследими решения.",
                "A clear purpose, agenda and traceable decisions.",
            ),
            0,
            listOf(
                LocalText("Определете целта", "Define the purpose"),
                LocalText("Изберете участници", "Choose participants"),
                LocalText("Уточнете дата и място", "Confirm date and location"),
                LocalText("Подгответе дневен ред", "Prepare the agenda"),
                LocalText("Споделете материалите", "Share supporting materials"),
                LocalText("Проверете техниката", "Check the equipment"),
                LocalText("Запишете решенията", "Record decisions"),
                LocalText("Разпределете следващите действия", "Assign follow-up actions"),
            ),
        ),
        Workflow(
            "template_2",
            LocalText("Адаптация на служител", "Employee onboarding"),
            LocalText(
                "Подготвено работно място и подредена първа седмица.",
                "A prepared workspace and an organized first week.",
            ),
            2,
            listOf(
                LocalText("Потвърдете началната дата", "Confirm the start date"),
                LocalText("Подгответе документите", "Prepare documents"),
                LocalText("Организирайте работното място", "Set up the workspace"),
                LocalText("Проверете необходимите достъпи", "Check required access"),
                LocalText("Изпратете програма за първия ден", "Send the first-day schedule"),
                LocalText("Представете екипа", "Introduce the team"),
                LocalText("Обяснете основните процеси", "Explain core processes"),
                LocalText("Определете първите задачи", "Define initial tasks"),
                LocalText("Насрочете обратна връзка", "Schedule feedback"),
                LocalText("Прегледайте първата седмица", "Review the first week"),
            ),
        ),
        Workflow(
            "template_3",
            LocalText("Организация на цифров архив", "Digital archive"),
            LocalText(
                "Подредени папки и предвидим достъп до документите.",
                "Organized folders and predictable access to documents.",
            ),
            3,
            listOf(
                LocalText("Съберете източниците", "Collect sources"),
                LocalText("Определете категории", "Define categories"),
                LocalText("Създайте структура от папки", "Create folder structure"),
                LocalText("Уточнете именуването", "Agree naming conventions"),
                LocalText("Премахнете дубликатите след проверка", "Review and remove duplicates"),
                LocalText("Проверете правата за достъп", "Check access permissions"),
                LocalText("Създайте резервно копие", "Create a backup"),
                LocalText("Проверете възстановяването", "Test restoration"),
            ),
        ),
        Workflow(
            "template_4",
            LocalText("Обработка на поща", "Inbox processing"),
            LocalText(
                "Поща с ясни действия и отделени справочни материали.",
                "Email with clear actions and separate reference material.",
            ),
            0,
            listOf(
                LocalText("Изберете време за обработка", "Choose processing time"),
                LocalText("Отделете нежеланата поща", "Separate unwanted mail"),
                LocalText("Групирайте по тема", "Group by topic"),
                LocalText("Отговорете на кратките въпроси", "Answer short questions"),
                LocalText("Превърнете останалото в задачи", "Turn remaining items into tasks"),
                LocalText("Уточнете чакащите отговори", "Identify pending replies"),
                LocalText("Архивирайте приключеното", "Archive completed threads"),
                LocalText("Планирайте следващия преглед", "Schedule the next review"),
            ),
        ),
        Workflow(
            "template_5",
            LocalText("Проверка на CRM", "CRM review"),
            LocalText(
                "Актуални контакти и по-малко дублирани записи.",
                "Current contacts and fewer duplicate records.",
            ),
            3,
            listOf(
                LocalText("Определете обхвата", "Define the scope"),
                LocalText("Експортирайте резервно копие", "Export a backup"),
                LocalText("Намерете непълните записи", "Find incomplete records"),
                LocalText("Проверете дубликатите", "Check duplicates"),
                LocalText("Потвърдете контактните данни", "Verify contact details"),
                LocalText("Обновете категориите", "Update categories"),
                LocalText("Проверете резултата", "Review the result"),
            ),
        ),
        Workflow(
            "template_6",
            LocalText("Месечна проверка на фактури", "Monthly invoice review"),
            LocalText(
                "Проверени фактури и видими несъответствия.",
                "Reviewed invoices and visible discrepancies.",
            ),
            4,
            listOf(
                LocalText("Съберете фактурите за периода", "Collect period invoices"),
                LocalText("Сверете номера и дати", "Check numbers and dates"),
                LocalText("Проверете сумите", "Check amounts"),
                LocalText("Сверете плащанията", "Match payments"),
                LocalText("Отбележете несъответствията", "Record discrepancies"),
                LocalText("Уточнете липсващите документи", "Request missing documents"),
                LocalText("Предайте въпросите на специалист", "Refer questions to a specialist"),
                LocalText("Архивирайте проверения набор", "Archive reviewed documents"),
            ),
        ),
        Workflow(
            "template_7",
            LocalText("Клиентски обращения", "Customer requests"),
            LocalText(
                "Всяко запитване има собственик и следващо действие.",
                "Every request has an owner and a next action.",
            ),
            1,
            listOf(
                LocalText("Съберете отворените запитвания", "Collect open requests"),
                LocalText("Групирайте по тема", "Group by topic"),
                LocalText("Потвърдете получаването", "Acknowledge receipt"),
                LocalText("Уточнете нужната информация", "Clarify missing information"),
                LocalText("Определете приоритет", "Set priority"),
                LocalText("Подгответе отговор", "Prepare a response"),
                LocalText("Потвърдете решението", "Confirm the resolution"),
                LocalText("Запишете следващите действия", "Record follow-up actions"),
            ),
        ),
        Workflow(
            "template_8",
            LocalText("Седмично планиране", "Weekly planning"),
            LocalText(
                "Реалистична седмица с време за важните задачи.",
                "A realistic week with room for important work.",
            ),
            0,
            listOf(
                LocalText("Прегледайте миналата седмица", "Review last week"),
                LocalText("Съберете всички задачи", "Collect outstanding tasks"),
                LocalText("Изберете три основни резултата", "Choose three key outcomes"),
                LocalText("Проверете календарните ангажименти", "Check calendar commitments"),
                LocalText("Разпределете сроковете", "Assign due dates"),
                LocalText("Оставете време за непредвидено", "Allow time for the unexpected"),
                LocalText("Планирайте седмичен преглед", "Schedule a weekly review"),
            ),
        ),
    )
val guides =
    listOf(
        Guide(
            "article_1",
            LocalText("Седмично планиране", "Weekly planning"),
            LocalText(
                "Планирайте по резултати, не по брой задачи.\n\nПреди да започнете\nРеалистична седмица с време за важните задачи. Отделете кратък, защитен период за тази работа. Съберете нужните документи и уточнете кои източници са актуални. Не копирайте лични данни в общодостъпни файлове.\n\nПрактически стъпки\n1. Прегледайте миналата седмица. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n2. Съберете всички задачи. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n3. Изберете три основни резултата. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n4. Проверете календарните ангажименти. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n5. Разпределете сроковете. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n6. Оставете време за непредвидено. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n7. Планирайте седмичен преглед. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\nПроверка на резултата\nСравнете постигнатото с началната цел. Проверете липсващите записи и неясните отговорности. Ако остане действие, създайте отделна задача със срок; не разчитайте на паметта си.\n\nПоддържане\nИзберете дата за следващ преглед. Адаптирайте стъпките към вашата организация и обсъдете специализираните кадрови, счетоводни и правни въпроси с компетентен специалист. Можете да приложите свързания шаблон локално и да промените всяка задача.",
                "Plan around outcomes rather than task counts.\n\nBefore you begin\nA realistic week with room for important work. Set aside a short protected period for this work. Gather the necessary documents and identify current sources. Keep personal information out of shared public files.\n\nPractical steps\n1. Review last week. Record the concrete outcome and capture open questions before moving on.\n\n2. Collect outstanding tasks. Record the concrete outcome and capture open questions before moving on.\n\n3. Choose three key outcomes. Record the concrete outcome and capture open questions before moving on.\n\n4. Check calendar commitments. Record the concrete outcome and capture open questions before moving on.\n\n5. Assign due dates. Record the concrete outcome and capture open questions before moving on.\n\n6. Allow time for the unexpected. Record the concrete outcome and capture open questions before moving on.\n\n7. Schedule a weekly review. Record the concrete outcome and capture open questions before moving on.\n\nCheck the result\nCompare the outcome with the original goal. Review missing records and unclear responsibilities. If an action remains, create a separate task with a due date rather than relying on memory.\n\nKeep it useful\nChoose a date for the next review. Adapt the steps to your organization and refer specialist employment, accounting and legal questions to a qualified professional. Apply the linked template locally and edit each task to fit your work.",
            ),
            "template_8",
            0,
            "article_1",
        ),
        Guide(
            "article_2",
            LocalText("Подготовка на среща", "Meeting preparation"),
            LocalText(
                "Добрата среща започва преди поканата.\n\nПреди да започнете\nЯсна цел, дневен ред и проследими решения. Отделете кратък, защитен период за тази работа. Съберете нужните документи и уточнете кои източници са актуални. Не копирайте лични данни в общодостъпни файлове.\n\nПрактически стъпки\n1. Определете целта. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n2. Изберете участници. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n3. Уточнете дата и място. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n4. Подгответе дневен ред. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n5. Споделете материалите. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n6. Проверете техниката. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n7. Запишете решенията. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n8. Разпределете следващите действия. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\nПроверка на резултата\nСравнете постигнатото с началната цел. Проверете липсващите записи и неясните отговорности. Ако остане действие, създайте отделна задача със срок; не разчитайте на паметта си.\n\nПоддържане\nИзберете дата за следващ преглед. Адаптирайте стъпките към вашата организация и обсъдете специализираните кадрови, счетоводни и правни въпроси с компетентен специалист. Можете да приложите свързания шаблон локално и да промените всяка задача.",
                "A useful meeting begins before the invitation.\n\nBefore you begin\nA clear purpose, agenda and traceable decisions. Set aside a short protected period for this work. Gather the necessary documents and identify current sources. Keep personal information out of shared public files.\n\nPractical steps\n1. Define the purpose. Record the concrete outcome and capture open questions before moving on.\n\n2. Choose participants. Record the concrete outcome and capture open questions before moving on.\n\n3. Confirm date and location. Record the concrete outcome and capture open questions before moving on.\n\n4. Prepare the agenda. Record the concrete outcome and capture open questions before moving on.\n\n5. Share supporting materials. Record the concrete outcome and capture open questions before moving on.\n\n6. Check the equipment. Record the concrete outcome and capture open questions before moving on.\n\n7. Record decisions. Record the concrete outcome and capture open questions before moving on.\n\n8. Assign follow-up actions. Record the concrete outcome and capture open questions before moving on.\n\nCheck the result\nCompare the outcome with the original goal. Review missing records and unclear responsibilities. If an action remains, create a separate task with a due date rather than relying on memory.\n\nKeep it useful\nChoose a date for the next review. Adapt the steps to your organization and refer specialist employment, accounting and legal questions to a qualified professional. Apply the linked template locally and edit each task to fit your work.",
            ),
            "template_1",
            0,
            "article_2",
        ),
        Guide(
            "article_3",
            LocalText("Обработка на поща", "Inbox processing"),
            LocalText(
                "Входящата поща е канал, а не списък със задачи.\n\nПреди да започнете\nПоща с ясни действия и отделени справочни материали. Отделете кратък, защитен период за тази работа. Съберете нужните документи и уточнете кои източници са актуални. Не копирайте лични данни в общодостъпни файлове.\n\nПрактически стъпки\n1. Изберете време за обработка. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n2. Отделете нежеланата поща. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n3. Групирайте по тема. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n4. Отговорете на кратките въпроси. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n5. Превърнете останалото в задачи. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n6. Уточнете чакащите отговори. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n7. Архивирайте приключеното. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n8. Планирайте следващия преглед. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\nПроверка на резултата\nСравнете постигнатото с началната цел. Проверете липсващите записи и неясните отговорности. Ако остане действие, създайте отделна задача със срок; не разчитайте на паметта си.\n\nПоддържане\nИзберете дата за следващ преглед. Адаптирайте стъпките към вашата организация и обсъдете специализираните кадрови, счетоводни и правни въпроси с компетентен специалист. Можете да приложите свързания шаблон локално и да промените всяка задача.",
                "An inbox is a communication channel, not a task list.\n\nBefore you begin\nEmail with clear actions and separate reference material. Set aside a short protected period for this work. Gather the necessary documents and identify current sources. Keep personal information out of shared public files.\n\nPractical steps\n1. Choose processing time. Record the concrete outcome and capture open questions before moving on.\n\n2. Separate unwanted mail. Record the concrete outcome and capture open questions before moving on.\n\n3. Group by topic. Record the concrete outcome and capture open questions before moving on.\n\n4. Answer short questions. Record the concrete outcome and capture open questions before moving on.\n\n5. Turn remaining items into tasks. Record the concrete outcome and capture open questions before moving on.\n\n6. Identify pending replies. Record the concrete outcome and capture open questions before moving on.\n\n7. Archive completed threads. Record the concrete outcome and capture open questions before moving on.\n\n8. Schedule the next review. Record the concrete outcome and capture open questions before moving on.\n\nCheck the result\nCompare the outcome with the original goal. Review missing records and unclear responsibilities. If an action remains, create a separate task with a due date rather than relying on memory.\n\nKeep it useful\nChoose a date for the next review. Adapt the steps to your organization and refer specialist employment, accounting and legal questions to a qualified professional. Apply the linked template locally and edit each task to fit your work.",
            ),
            "template_4",
            0,
            "article_3",
        ),
        Guide(
            "article_4",
            LocalText("Адаптация на служител", "Employee onboarding"),
            LocalText(
                "Ясният първи ден намалява излишните въпроси.\n\nПреди да започнете\nПодготвено работно място и подредена първа седмица. Отделете кратък, защитен период за тази работа. Съберете нужните документи и уточнете кои източници са актуални. Не копирайте лични данни в общодостъпни файлове.\n\nПрактически стъпки\n1. Потвърдете началната дата. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n2. Подгответе документите. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n3. Организирайте работното място. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n4. Проверете необходимите достъпи. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n5. Изпратете програма за първия ден. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n6. Представете екипа. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n7. Обяснете основните процеси. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n8. Определете първите задачи. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n9. Насрочете обратна връзка. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n10. Прегледайте първата седмица. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\nПроверка на резултата\nСравнете постигнатото с началната цел. Проверете липсващите записи и неясните отговорности. Ако остане действие, създайте отделна задача със срок; не разчитайте на паметта си.\n\nПоддържане\nИзберете дата за следващ преглед. Адаптирайте стъпките към вашата организация и обсъдете специализираните кадрови, счетоводни и правни въпроси с компетентен специалист. Можете да приложите свързания шаблон локално и да промените всяка задача.",
                "A clear first day reduces unnecessary questions.\n\nBefore you begin\nA prepared workspace and an organized first week. Set aside a short protected period for this work. Gather the necessary documents and identify current sources. Keep personal information out of shared public files.\n\nPractical steps\n1. Confirm the start date. Record the concrete outcome and capture open questions before moving on.\n\n2. Prepare documents. Record the concrete outcome and capture open questions before moving on.\n\n3. Set up the workspace. Record the concrete outcome and capture open questions before moving on.\n\n4. Check required access. Record the concrete outcome and capture open questions before moving on.\n\n5. Send the first-day schedule. Record the concrete outcome and capture open questions before moving on.\n\n6. Introduce the team. Record the concrete outcome and capture open questions before moving on.\n\n7. Explain core processes. Record the concrete outcome and capture open questions before moving on.\n\n8. Define initial tasks. Record the concrete outcome and capture open questions before moving on.\n\n9. Schedule feedback. Record the concrete outcome and capture open questions before moving on.\n\n10. Review the first week. Record the concrete outcome and capture open questions before moving on.\n\nCheck the result\nCompare the outcome with the original goal. Review missing records and unclear responsibilities. If an action remains, create a separate task with a due date rather than relying on memory.\n\nKeep it useful\nChoose a date for the next review. Adapt the steps to your organization and refer specialist employment, accounting and legal questions to a qualified professional. Apply the linked template locally and edit each task to fit your work.",
            ),
            "template_2",
            2,
            "article_4",
        ),
        Guide(
            "article_5",
            LocalText("Проверка на CRM", "CRM review"),
            LocalText(
                "Качеството на CRM зависи от последователни правила.\n\nПреди да започнете\nАктуални контакти и по-малко дублирани записи. Отделете кратък, защитен период за тази работа. Съберете нужните документи и уточнете кои източници са актуални. Не копирайте лични данни в общодостъпни файлове.\n\nПрактически стъпки\n1. Определете обхвата. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n2. Експортирайте резервно копие. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n3. Намерете непълните записи. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n4. Проверете дубликатите. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n5. Потвърдете контактните данни. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n6. Обновете категориите. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n7. Проверете резултата. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\nПроверка на резултата\nСравнете постигнатото с началната цел. Проверете липсващите записи и неясните отговорности. Ако остане действие, създайте отделна задача със срок; не разчитайте на паметта си.\n\nПоддържане\nИзберете дата за следващ преглед. Адаптирайте стъпките към вашата организация и обсъдете специализираните кадрови, счетоводни и правни въпроси с компетентен специалист. Можете да приложите свързания шаблон локално и да промените всяка задача.",
                "CRM quality depends on consistent rules.\n\nBefore you begin\nCurrent contacts and fewer duplicate records. Set aside a short protected period for this work. Gather the necessary documents and identify current sources. Keep personal information out of shared public files.\n\nPractical steps\n1. Define the scope. Record the concrete outcome and capture open questions before moving on.\n\n2. Export a backup. Record the concrete outcome and capture open questions before moving on.\n\n3. Find incomplete records. Record the concrete outcome and capture open questions before moving on.\n\n4. Check duplicates. Record the concrete outcome and capture open questions before moving on.\n\n5. Verify contact details. Record the concrete outcome and capture open questions before moving on.\n\n6. Update categories. Record the concrete outcome and capture open questions before moving on.\n\n7. Review the result. Record the concrete outcome and capture open questions before moving on.\n\nCheck the result\nCompare the outcome with the original goal. Review missing records and unclear responsibilities. If an action remains, create a separate task with a due date rather than relying on memory.\n\nKeep it useful\nChoose a date for the next review. Adapt the steps to your organization and refer specialist employment, accounting and legal questions to a qualified professional. Apply the linked template locally and edit each task to fit your work.",
            ),
            "template_5",
            3,
            "article_5",
        ),
        Guide(
            "article_6",
            LocalText("Организация на цифров архив", "Digital archive"),
            LocalText(
                "Полезният архив позволява бързо намиране и безопасно възстановяване.\n\nПреди да започнете\nПодредени папки и предвидим достъп до документите. Отделете кратък, защитен период за тази работа. Съберете нужните документи и уточнете кои източници са актуални. Не копирайте лични данни в общодостъпни файлове.\n\nПрактически стъпки\n1. Съберете източниците. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n2. Определете категории. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n3. Създайте структура от папки. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n4. Уточнете именуването. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n5. Премахнете дубликатите след проверка. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n6. Проверете правата за достъп. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n7. Създайте резервно копие. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n8. Проверете възстановяването. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\nПроверка на резултата\nСравнете постигнатото с началната цел. Проверете липсващите записи и неясните отговорности. Ако остане действие, създайте отделна задача със срок; не разчитайте на паметта си.\n\nПоддържане\nИзберете дата за следващ преглед. Адаптирайте стъпките към вашата организация и обсъдете специализираните кадрови, счетоводни и правни въпроси с компетентен специалист. Можете да приложите свързания шаблон локално и да промените всяка задача.",
                "A useful archive supports quick retrieval and safe restoration.\n\nBefore you begin\nOrganized folders and predictable access to documents. Set aside a short protected period for this work. Gather the necessary documents and identify current sources. Keep personal information out of shared public files.\n\nPractical steps\n1. Collect sources. Record the concrete outcome and capture open questions before moving on.\n\n2. Define categories. Record the concrete outcome and capture open questions before moving on.\n\n3. Create folder structure. Record the concrete outcome and capture open questions before moving on.\n\n4. Agree naming conventions. Record the concrete outcome and capture open questions before moving on.\n\n5. Review and remove duplicates. Record the concrete outcome and capture open questions before moving on.\n\n6. Check access permissions. Record the concrete outcome and capture open questions before moving on.\n\n7. Create a backup. Record the concrete outcome and capture open questions before moving on.\n\n8. Test restoration. Record the concrete outcome and capture open questions before moving on.\n\nCheck the result\nCompare the outcome with the original goal. Review missing records and unclear responsibilities. If an action remains, create a separate task with a due date rather than relying on memory.\n\nKeep it useful\nChoose a date for the next review. Adapt the steps to your organization and refer specialist employment, accounting and legal questions to a qualified professional. Apply the linked template locally and edit each task to fit your work.",
            ),
            "template_3",
            3,
            "article_6",
        ),
        Guide(
            "article_7",
            LocalText("Месечна проверка на фактури", "Monthly invoice review"),
            LocalText(
                "Подредените документи правят месечния преглед по-ясен.\n\nПреди да започнете\nПроверени фактури и видими несъответствия. Отделете кратък, защитен период за тази работа. Съберете нужните документи и уточнете кои източници са актуални. Не копирайте лични данни в общодостъпни файлове.\n\nПрактически стъпки\n1. Съберете фактурите за периода. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n2. Сверете номера и дати. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n3. Проверете сумите. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n4. Сверете плащанията. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n5. Отбележете несъответствията. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n6. Уточнете липсващите документи. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n7. Предайте въпросите на специалист. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n8. Архивирайте проверения набор. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\nПроверка на резултата\nСравнете постигнатото с началната цел. Проверете липсващите записи и неясните отговорности. Ако остане действие, създайте отделна задача със срок; не разчитайте на паметта си.\n\nПоддържане\nИзберете дата за следващ преглед. Адаптирайте стъпките към вашата организация и обсъдете специализираните кадрови, счетоводни и правни въпроси с компетентен специалист. Можете да приложите свързания шаблон локално и да промените всяка задача.",
                "Organized documents make monthly review clearer.\n\nBefore you begin\nReviewed invoices and visible discrepancies. Set aside a short protected period for this work. Gather the necessary documents and identify current sources. Keep personal information out of shared public files.\n\nPractical steps\n1. Collect period invoices. Record the concrete outcome and capture open questions before moving on.\n\n2. Check numbers and dates. Record the concrete outcome and capture open questions before moving on.\n\n3. Check amounts. Record the concrete outcome and capture open questions before moving on.\n\n4. Match payments. Record the concrete outcome and capture open questions before moving on.\n\n5. Record discrepancies. Record the concrete outcome and capture open questions before moving on.\n\n6. Request missing documents. Record the concrete outcome and capture open questions before moving on.\n\n7. Refer questions to a specialist. Record the concrete outcome and capture open questions before moving on.\n\n8. Archive reviewed documents. Record the concrete outcome and capture open questions before moving on.\n\nCheck the result\nCompare the outcome with the original goal. Review missing records and unclear responsibilities. If an action remains, create a separate task with a due date rather than relying on memory.\n\nKeep it useful\nChoose a date for the next review. Adapt the steps to your organization and refer specialist employment, accounting and legal questions to a qualified professional. Apply the linked template locally and edit each task to fit your work.",
            ),
            "template_6",
            4,
            "article_7",
        ),
        Guide(
            "article_8",
            LocalText("Клиентски обращения", "Customer requests"),
            LocalText(
                "Последователният отговор създава предвидимост за клиента.\n\nПреди да започнете\nВсяко запитване има собственик и следващо действие. Отделете кратък, защитен период за тази работа. Съберете нужните документи и уточнете кои източници са актуални. Не копирайте лични данни в общодостъпни файлове.\n\nПрактически стъпки\n1. Съберете отворените запитвания. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n2. Групирайте по тема. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n3. Потвърдете получаването. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n4. Уточнете нужната информация. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n5. Определете приоритет. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n6. Подгответе отговор. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n7. Потвърдете решението. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\n8. Запишете следващите действия. Отбележете конкретния резултат и запишете отворените въпроси, преди да продължите.\n\nПроверка на резултата\nСравнете постигнатото с началната цел. Проверете липсващите записи и неясните отговорности. Ако остане действие, създайте отделна задача със срок; не разчитайте на паметта си.\n\nПоддържане\nИзберете дата за следващ преглед. Адаптирайте стъпките към вашата организация и обсъдете специализираните кадрови, счетоводни и правни въпроси с компетентен специалист. Можете да приложите свързания шаблон локално и да промените всяка задача.",
                "A consistent response gives customers predictability.\n\nBefore you begin\nEvery request has an owner and a next action. Set aside a short protected period for this work. Gather the necessary documents and identify current sources. Keep personal information out of shared public files.\n\nPractical steps\n1. Collect open requests. Record the concrete outcome and capture open questions before moving on.\n\n2. Group by topic. Record the concrete outcome and capture open questions before moving on.\n\n3. Acknowledge receipt. Record the concrete outcome and capture open questions before moving on.\n\n4. Clarify missing information. Record the concrete outcome and capture open questions before moving on.\n\n5. Set priority. Record the concrete outcome and capture open questions before moving on.\n\n6. Prepare a response. Record the concrete outcome and capture open questions before moving on.\n\n7. Confirm the resolution. Record the concrete outcome and capture open questions before moving on.\n\n8. Record follow-up actions. Record the concrete outcome and capture open questions before moving on.\n\nCheck the result\nCompare the outcome with the original goal. Review missing records and unclear responsibilities. If an action remains, create a separate task with a due date rather than relying on memory.\n\nKeep it useful\nChoose a date for the next review. Adapt the steps to your organization and refer specialist employment, accounting and legal questions to a qualified professional. Apply the linked template locally and edit each task to fit your work.",
            ),
            "template_7",
            1,
            "article_8",
        ),
    )
