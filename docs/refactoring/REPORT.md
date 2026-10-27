# Дословный рефакторинг NEW LOOK

## Что изменено

- `data/Entities.kt`: из `Database.kt` перенесён дословный блок `uuid()` и моделей Project, Task, Subtask, Event, Note, Series, Favorite, UserTemplate.
- `ui/TaskEditor.kt`: TaskEditor и SnoozeControls.
- `ui/ProjectEditor.kt`, `ui/EventEditor.kt`, `ui/NoteEditor.kt`: соответствующие редакторы.
- В исходных файлах удалены только перенесённые блоки и относящиеся к ним импорты. Новые файлы содержат заголовки package/import/OptIn и исходные блоки. Общие компоненты форм остаются в Editors.kt; DAO, миграция и WorkspaceDb — в Database.kt.

## Доказательство

Проект не Safora. Проверка `verify_preservation.py` специально сопоставляет фактические границы NEW LOOK из `blocks.json` и `before.zip`; код проверки Safora не использовался как универсальный. Шесть блоков совпадают побайтово, без нормализации пробелов или комментариев. SHA-256 до/после записаны в `preservation.json`. Остатки исходных файлов проверены точно, допустимые удаления импортов перечислены в `removed-imports.json`. Все остальные 76 файлов архива неизменны; новых исходников/ресурсов/тестов сверх пяти объявленных файлов нет. Room schema 2 совпадает с исходной схемой в ранее сохранённом source-bundle.

## Выполненные проверки сейчас

1. До правок: assembleDebug, testDebugUnitTest, lintDebug — PASS (`logs/baseline.log`).
2. После моделей: assembleDebug, testDebugUnitTest — PASS (`logs/after-entities.log`).
3. После всех переносов: assembleDebug, testDebugUnitTest, lintDebug — PASS (`logs/final.log`). Все 20 unit-тестов прошли; XML сохранены в test-results.
4. Повторный запуск `python3 docs/refactoring/verify_preservation.py` — PASS.
5. Debug APK до/после проверены apksigner. ApplicationId, versionCode/versionName, min/target SDK, список нативных ABI и SHA-256 сертификата подписи совпадают (`apk-verification.json`, публичные сведения — logs/*signature.txt и *badging.txt).

## Границы

Git-репозитория/истории и применимых AGENTS.md / CLAUDE.md не найдено. Сравнение опирается на исходный архив; коммиты не создавались. Алгоритмы, UI, сообщения, данные, пути, JSON-ключи, Room-схема/миграции, Android-компоненты, Gradle-конфигурация, зависимости, SDK, версия и ключ подписи не менялись. Различающиеся SHA-256 APK ожидаемы при изменении размещения Kotlin-деклараций; бинарная идентичность APK не заявляется.

UI/instrumentation-тесты в этом этапе НЕ запускались; прошлые прогоны не считаются новым доказательством. Эмулятор и пользовательские данные не изменялись. Release APK/AAB не собирались: release не запрошен, существующие ключи не заменялись.

## Артефакты

before.zip / after.zip — исходники и конфигурация без signing-файлов; before-debug.apk / after-debug.apk — debug APK до/после; refactor.patch — unified diff; preservation.json — побайтовые сравнения; apk-verification.json — параметры APK; logs и test-results — проверки. Сборка также сохранена в output/refactoring/newlook-refactored-debug.apk.
