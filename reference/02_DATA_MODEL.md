# Модель данных и инварианты

Это контракт поведения, а не готовые Room entities. Агент выбирает корректные SQL-типы и миграции.

| Сущность | Основные поля и связи |
|---|---|
| Project | id UUID, title, description, colorToken, targetLocalDate?, archivedAt?, createdAt, updatedAt |
| Task | id, projectId?, title, description, status, priority, dueLocalDate?, dueLocalTime?, zoneId?, seriesId?, occurrenceKey?, completedAt?, createdAt, updatedAt |
| Subtask | id, taskId, title, position, done |
| Tag / TaskTag | id, name, colorToken / taskId + tagId composite PK |
| RecurrenceSeries | id, taskPrototype, frequency, interval, weekdays, monthDay?, zoneId, startLocalDate, endLocalDate?, occurrenceLimit?, active, generatedThrough |
| Reminder | id, ownerType task/event, ownerId, triggerSpec, enabled; polymorphic integrity проверяется транзакцией |
| Event | id, title, allDay, startInstant/endInstant либо startDate/endDateExclusive, zoneId, location, notes, projectId? |
| Note | id, title, body, pinned, projectId?, createdAt, updatedAt |
| NoteTag | noteId, tagId |
| Template | id, origin built_in/user, contentKey? либо userTitle, description, steps, relatedServiceIds |
| TemplateStep | id, templateId, titleKey/userTitle, relativeDays, position, priority, subtasks |
| Service / Article | стабильный contentId, BG/EN поля, imageId, category, связи |
| Favorite | type service/article/template + targetId composite PK |
| ImageAsset | id, filename, ownerContentId, prompt, generationStatus |

## Обязательные инварианты

- Task done ⇔ completedAt != null; статусы изменяются одним domain action.
- LocalTime срока не существует без LocalDate. Срок без времени не преобразуется в UTC-полночь.
- Событие allDay использует даты и исключительную конечную дату; обычное событие — абсолютные моменты + зону.
- UNIQUE(seriesId, occurrenceKey) для экземпляров; транзакционная генерация повторов.
- Архивный проект скрыт в активных, но доступен через фильтр и не удаляет содержимое.
- Удаление Task удаляет Subtask, TaskTag, Reminder и поисковую запись; планировщик очищает внешние alarms.
- Удаление пользовательского шаблона удаляет его Favorite. Удаление шаблона не удаляет созданные из него проекты.
- Строки локализации не являются ключами. Пользовательские тексты остаются исходными.
- JSON backup содержит UUID, связи, версии и правила повторений; platform alarm IDs не считаются переносимыми.
- Импорт делается в транзакции после полной валидации; reconciler восстанавливает производные состояния.

## Сценарий сквозного использования

Пользователь применяет «Адаптация сотрудника» на выбранную дату → создаются проект и задачи с относительными сроками → меняет один срок → задаёт уведомление → выполняет подзадачи → видит прогресс → сохраняет заметку проекта → экспортирует backup → импортирует на чистую установку → получает прежние записи и заново рассчитанные уведомления.
