# NEW LOOK: генерация upload key и AAB в Codemagic

Адаптировано из исправленного корневого YAML Safora. Подтверждённый пользователем облачный результат Safora: build №2, commit dcadecc. Это не результат облачной сборки NEW LOOK.

## Параметры NEW LOOK

- applicationId: `bg.newlook.organizer`; модуль `app`, `app/build.gradle.kts`.
- Java 17; compileSdk 37, targetSdk 36, minSdk 26; versionCode 2, versionName 1.1.0.
- Четыре ABI: armeabi-v7a, arm64-v8a, x86, x86_64.
- Gradle читает `release/upload-key.properties`; `storeFile` разрешается относительно корня проекта.
- Уже существуют локальные `release/signing/newlook-upload.jks` и соответствующий PEM. Они не заменены. Наличие зарегистрированного upload key в Play Console из локальных файлов определить нельзя.
- `isMinifyEnabled = false`: mapping.txt сейчас не создаётся. Включать R8 ради mapping не требуется; если минификация появится, сохраняйте mapping именно соответствующего билда.

## 1. Проверьте регистрацию ключа

Если приложение уже загружалось в Play Console, сверяйте Upload key certificate и используйте зарегистрированный ключ. Для существующего локального ключа alias — `newlook-upload`; загрузите его в identities и пропустите генератор. Новый ключ после регистрации старого требует процедуры сброса upload key в Play Console.

Если ключ ещё не зарегистрирован и вы хотите создать его именно в облаке, выполните одноразовый workflow ниже. Сборка прежнего локального AAB сама по себе не подтверждает регистрацию в Play.

## 2. Подготовьте Git

Сейчас рабочая папка не является Git-репозиторием. Нужен репозиторий в вашем GitHub/GitLab/Bitbucket и подключение его в Codemagic Applications → Add application. Публикация исходников и создание remote этим изменением не выполнялись.

Добавьте исходники и конфигурацию, а не весь каталог целиком. Пример из корня проекта:

```sh
git init
git branch -M main
git add .gitignore build.gradle.kts settings.gradle.kts gradle.properties gradlew gradlew.bat gradle app/build.gradle.kts app/src docs/codemagic codemagic.yaml
git diff --cached --name-only
git commit -m "Configure Codemagic signing workflows"
git remote add origin YOUR_REPOSITORY_URL
git push -u origin main
```

Подставьте URL вашего репозитория. Не добавляйте JKS, keystore, приватные properties, local.properties, APK/AAB, архивы и каталоги с результатами. .gitignore дополнен; он не удаляет файлы, если они ранее были закоммичены. Приватный локальный debug keystore теперь необязателен: в чистом CI Android использует стандартный debug key.

## 3. Одноразовая генерация на Codemagic

Корневой `codemagic.yaml` сейчас содержит ТОЛЬКО генератор `newlook-create-upload-key-once`. Сохранённая копия: `docs/codemagic/generate-upload-key.yaml`.

1. В Environment variables создайте группу `newlook_key_generation`.
2. Добавьте `NEW_KEYSTORE_PASSWORD` и `NEW_KEY_PASSWORD`; у обоих включите Secret. Сохраните пароли отдельно в менеджере паролей.
3. В YAML указан `KEY_SUBJECT: "CN=NEW LOOK"` — имя бренда без вымышленных данных организации. При необходимости замените его фактическими данными владельца сертификата до запуска.
4. Отправьте YAML в Git. В Codemagic выберите нужную ветку → Start new build → NEW LOOK - ONE-TIME create upload key.
5. Workflow создаст `newlook-upload.jks`: alias `upload`, JKS, RSA 4096, SHA256withRSA, 10000 дней. Затем экспортирует публичный `upload_certificate.pem`.
6. Скачайте оба артефакта и сохраните резервную копию JKS. Ссылки на приватный JKS не публикуйте.

Генерация одновременно создаёт keystore и ключ. Не запускайте её для каждой версии. Защита от перезаписи действует в пределах checkout, но повторный запуск на чистой build-машине создаст ДРУГОЙ ключ.

## 4. Загрузите JKS в identities

Team settings → codemagic.yaml settings → Code signing identities → Android keystores:

| Поле | Новый облачный ключ |
|---|---|
| File | скачанный newlook-upload.jks |
| Keystore password | NEW_KEYSTORE_PASSWORD |
| Key alias | upload |
| Key password | NEW_KEY_PASSWORD |
| Reference name | newlook_upload |

Alias и Reference name — разные значения. Для прежнего локального ключа alias `newlook-upload`, а не `upload`. Сохраните резервную копию: identities не заменяет собственное хранилище ключа.

## 5. Переключитесь на обычный AAB workflow

После сохранения и загрузки JKS:

```sh
cp docs/codemagic/codemagic.yaml codemagic.yaml
git add codemagic.yaml docs/codemagic app/build.gradle.kts .gitignore
git commit -m "Enable signed NEW LOOK AAB builds"
git push
```

Обычный workflow `newlook-aab` использует identity `newlook_upload`. Codemagic предоставляет CM_KEYSTORE_PATH, CM_KEYSTORE_PASSWORD, CM_KEY_ALIAS, CM_KEY_PASSWORD; вручную копировать эти пароли в YAML не нужно.

Workflow на Linux X2:

1. Находит sdkmanager по полному пути внутри SDK; устанавливает platforms;android-37 и build-tools;36.0.0. Не полагается на sdkmanager в PATH.
2. `prepare_signing.py` создаёт `release/signing/upload-keystore.jks` и `release/upload-key.properties` с правами 600. Значение storeFile — `release/signing/upload-keystore.jks`. Скрипт проверяет CM_* и отказывается перезаписывать файлы; пароли не выводятся.
3. Выполняет `./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:bundleRelease`.
4. `verify_aab.py` проверяет JAR-подпись AAB, совпадение сертификата с выбранным identity, все четыре ABI, отсутствие приватных signing-файлов внутри bundle.

Обычный workflow не возвращает JKS и не отправляет приложение в Google Play. Не включайте set -x, printenv или вывод приватных properties для диагностики.

## 6. Запуск и результаты

В Codemagic выберите ветку с обычным YAML → Start new build → NEW LOOK - Signed AAB. После реального успеха скачайте:

- `app-release.aab`;
- отчёт lint и unit tests;
- mapping.txt, если R8 включён в соответствующей версии (сейчас выключен).

Локальный путь AAB: `app/build/outputs/bundle/release/app-release.aab`.

Перед следующей загрузкой в Google Play versionCode должен быть больше уже использованного: если 2 загружался, увеличьте до свободного значения и пересоберите. Для нового приложения App signing key можно поручить Google; upload key остаётся вашим ключом загрузки.

Локальные результаты адаптации записаны в `local-validation.json`. Они не подтверждают облачный запуск NEW LOOK. Результат облака — только статус и артефакты реального билда Codemagic.
