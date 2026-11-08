# VLOM — Vista Launcher Over Mess

![CI](https://github.com/stelgen/vlom/actions/workflows/ci.yml/badge.svg)
![Phase](https://img.shields.io/badge/roadmap-%D1%84%D0%B0%D0%B7%D0%B0%201%20(MCML%20%D0%B4%D0%B2%D0%B8%D0%B6%D0%BE%D0%BA)-blue)
![minSdk](https://img.shields.io/badge/minSdk-28%20(Android%209)-green)
![ABI](https://img.shields.io/badge/APK-universal%20arm32%2Barm64-orange)
![Telemetry](https://img.shields.io/badge/%D1%82%D0%B5%D0%BB%D0%B5%D0%BC%D0%B5%D1%82%D1%80%D0%B8%D1%8F-%D0%BD%D0%BE%D0%BB%D1%8C-red)

**Vista Launcher Over Mess** — пиксель-верная реконструкция оболочки **Windows Vista Media
Center** (не Win7/XP) как системного HOME-лаунчера для Android TV 9.0+.
«Mess» — всё то, что кодят вендоры: трекеры, отсутствие оптимизации, реклама.
VLOM заменяет это на оригинальный VISTA MCE: zero-телеметрия, zero-GMS, 1:1 к оригиналу.

> 🔒 Закрытый личный проект (v1). ТЗ и роадмап: протокол **REQ-001**.

---

## Архитектура

```
┌────────────────────────────────────────────────────────────┐
│ MainActivity (HOME: CATEGORY_HOME + LEANBACK_LAUNCHER)     │
└──────────────┬─────────────────────────────────────────────┘
               │
┌──────────────▼─────────────────────────────────────────────┐
│ Фаза 1 — MCML-движок (реконструкция MCML-рендера ehshell)  │
│                                                            │
│  mcml/McmlParser   XML → McmlElement (DOM, ns-aware)       │
│  mcml/render/*     McmlElement → RenderNode (типизирован.) │
│  ui/McmlView       View: загрузка страницы + Canvas-рендер │
│  DesignScale       базис 1366×768 → экран (letterbox)      │
│  McmlRenderer      Canvas: панели/текст/кнопки, палитра    │
└──────────────┬─────────────────────────────────────────────┘
               │
┌──────────────▼─────────────────────────────────────────────┐
│ Assets (31 МБ, полный пакет темы Blue)                     │
│  mcml/blue/   470 MCML + 54 XML + 158 txt + 156 bin        │
│  images/blue/ 2325 PNG                                     │
│  audio/       WAV навигации (Jelly, Launch)                │
└────────────────────────────────────────────────────────────┘
```

**Стек:** Kotlin · Canvas (без Compose — контроль пикселя) · javax DOM (MCML) ·
SoundPool (WAV, фаза 1) · minSdk 28 · universal APK (arm32-v7a + arm64-v8a) · JDK 17.

## Структура репозитория

| Путь | Назначение |
|---|---|
| `app/src/main/java/com/stelgen/vlom/mcml/` | Парсер MCML → модель → JSON |
| `app/src/main/java/com/stelgen/vlom/mcml/render/` | Рендер-модель, масштабирование, Canvas |
| `app/src/main/java/com/stelgen/vlom/ui/` | `McmlView` (интеграция движка в View) |
| `app/src/main/assets/` | Полный пакет ресурсов Vista MCE (Blue) |
| `app/src/test/` | Unit-тесты: корпус реальных сэмплов blue-full |
| `.github/workflows/ci.yml` | Build + unit-тесты + gitleaks на каждый push/PR |
| `.github/workflows/release-screenshot.yml` | Релиз → эмулятор Android TV 9 → скриншот → ассет к Release |

## Сборка

```bash
git clone git@github.com:stelgen/vlom.git && cd vlom
echo "sdk.dir=$HOME/tools/android-sdk" > local.properties   # не коммитится
export JAVA_HOME=$HOME/tools/jdk-17.0.13+11
./gradlew assembleDebug testDebugUnitTest
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Требования: JDK 17, Android SDK (platform 34). Первый запуск Gradle качает зависимости.

## Процессы разработки (канон)

- **VCS:** git + [jj](https://jj-vcs.github.io/jj/) colocated (`jj` — рабочий процесс, git — push/CI).
  Канон коммита: `jj file track .` → `jj describe` → `jj bookmark set main -r @` → `git push`.
- **CI:** каждый push = сборка universal APK + unit-тесты + сканирование секретов (gitleaks).
- **Релизы:** тег `v*` → GitHub Release → автоматом собирается APK, поднимается эмулятор
  Android TV 9 (API 28), приложение ставится как HOME, делается скриншот и прикрепляется к релизу.
- ⚠️ **Грабли (проверено):**
  - коммит, которым ставится тег релиза, не должен содержать `[skip ci]` нигде в сообщении;
  - воркфлоу, добавленные тем же пушем, сами себя не триггерят — нужен следующий пуш;
  - jj 0.46 не автотрекает новые файлы — забыл `jj file track .` → коммит уйдёт пустым.

## Роадмап (REQ-001)

| Фаза | Содержание | Статус |
|---|---|---|
| **0** | Фундамент: Gradle, ресурсы, парсер, скелет HOME | ✅ (CI green) |
| **1** | Ядро MCML-движка: RenderNode, Canvas-масштаб, D-pad, анимации+звук | 🚧 в работе |
| **2** | Shell v1: старт-скрин, тайтлы, запуск приложений, настройки | ⏳ |
| **3** | Тайтлы-сервисы: погода (Open-Meteo), Frigate, веб-тайтлы | ⏳ |
| **4** | Верность 1:1: скринтюнинг, звуковая карта, темы Black/Green | ⏳ |
| **5** | Стабилизация: low-RAM, отказоустойчивость HOME, релиз v1.0 | ⏳ |

Детали: полный ТЗ — `/home/ot/wmc-artifacts/REQ-001-roadmap.md` (вне репо).

## Целевые устройства

Xiaomi 32 (32-бит) · Thomson R3 · XGIMI проектор — от 3.5 ГБ RAM, Android TV 9.0+.

## Лицензия и ограничения

Частный проект. Ресурсы Vista MCE извлечены из дистрибутива Microsoft для личного
использования; шрифты Microsoft в APK запрещены — только метрически совместимые
open-source (Selawik и др.). Ни телеметрии, ни сбора данных — никогда.
