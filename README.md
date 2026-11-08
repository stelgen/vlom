<div align="center">

# VLOM — Vista Launcher Over Mess

**Windows Vista Media Center, воскрешённый на Android TV.**

Замена рекламного лаунчера вендора — на пиксель-верную реконструкцию оболочки
Vista MCE (2005): тайлы, звуки, анимации. Без трекеров. Без рекламы. Без Google-сервисов.

[![CI](https://github.com/stelgen/vlom/actions/workflows/ci.yml/badge.svg)](https://github.com/stelgen/vlom/actions/workflows/ci.yml)
![minSdk](https://img.shields.io/badge/Android-TV%209.0%2B-green)
![ABI](https://img.shields.io/badge/APK-arm32%20%2B%20arm64-orange)
![Telemetry](https://img.shields.io/badge/телеметрия-ноль-red)
![License](https://img.shields.io/badge/лицензия-частный%20проект-lightgrey)

<img src="docs/screenshot.png" alt="Старт-скрин VLOM" width="640"/>

*Скриншот обновляется автоматически после каждого релиза (эмулятор Android TV 9).*

</div>

---

## Что это

Vendорские ТВ-лаунчеры — это «mess»: трекеры, реклама, тяжёлые оболочки на слабом железе.
**VLOM** полностью заменяет системный HOME на Android TV реконструкцией **Windows Vista
Media Center**: оригинальная разметка MCML (470 страниц, извлечены из ehres.dll),
оригинальные звуки навигации, темы Blue/Black/Green — всё 1:1 к оригиналу.

- **Ноль телеметрии. Ноль GMS.** Никакого сбора данных — никогда.
- **Лёгкий:** Canvas-рендер собственного MCML-движка, работает на ТВ с 3.5 ГБ RAM.
- **Универсальный APK:** arm32-v7a + arm64-v8a в одном файле.

## Возможности

| | |
|---|---|
| 🏠 **HOME-замена** | Перехват кнопки Home (`CATEGORY_HOME`), старт-скрин в стиле Vista MCE |
| 🎛️ **Тайтлы** | Оригинальные (Музыка, Картинки, Видео, Наблюдение, Погода) + любые установленные APK и сайты |
| ⚙️ **Настройки** | Добавление/скрытие/сортировка тайтлов, смена темы — с пульта (D-pad) |
| 🌦️ **Погода** | Open-Meteo, без ключа; гео по IP, смена города |
| 📹 **Frigate** | Веб-морда видеонаблюдения внутри лаунчера (NVR) |
| 🌐 **Веб-тайтлы** | Любые сайты, WebGL 2.0 из коробки |
| 🎨 **Темы** | Blue (оригинал) / Black / Green — переключение в настройках |
| 🔊 **Звуки** | Оригинальные WAV Vista MCE (Jelly UI), SoundPool |

## Установка

1. Скачайте APK со страницы [Releases](https://github.com/stelgen/vlom/releases).
2. Разрешите установку неизвестных приложений на ТВ-устройстве.
3. При первом нажатии Home выберите **Vista (VLOM)** как домашний экран.

> Резервный вход в стоковый лаунчер сохраняется — VLOM не блокирует ничего.

## Устройства

| Устройство | Примечание |
|---|---|
| **Xiaomi TV A2 32** (L32M7-EARU) | первичное: HD 1366×768 = базис MCML, рендер 1:1, 32-бит ABI |
| Thomson R3 | проверка масштабирования |
| XGIMI (проектор) | проверка масштабирования |

Требования: Android TV 9.0+ (API 28+), от 3.5 ГБ RAM.

## Для разработчиков

```bash
git clone git@github.com:stelgen/vlom.git && cd vlom
echo "sdk.dir=$ANDROID_HOME" > local.properties   # не коммитится
export JAVA_HOME=$JAVA_HOME_17
./gradlew assembleDebug testDebugUnitTest
```

**Стек:** Kotlin · Canvas (без Compose — контроль пикселя) · javax DOM (MCML) · SoundPool ·
JDK 17 · Gradle 8.9 · AGP 8.5.2.

**Роадмап:** фазы 0–3 (фундамент, MCML-движок, shell, сервисы) — реализованы;
фаза 4 — верность 1:1 (скринтюнинг, скины из ресурсов ehres); фаза 5 — стабилизация и v1.0.
Полное ТЗ: протокол REQ-001 (не публикуется).

**Процессы:**
- каждый push: сборка universal APK + unit-тесты + сканирование секретов (gitleaks)
- тег `v*` → подписанный APK + GitHub Release с автонотами
- после каждого релиза: эмулятор Android TV 9 ставит APK как HOME → скриншот →
  автоматически подставляется в этот README (`docs/screenshot.png`) и прикладывается к релизу
- VCS: git + [jj](https://jj-vcs.github.io/jj/) colocated

## Лицензия и ограничения

Частный проект. Ресурсы Vista MCE извлечены из дистрибутива Microsoft для личного
использования; шрифты Microsoft не распространяются — используется
[Selawik](https://github.com/microsoft/Selawik) (SIL OFL, метрики Segoe UI).
Сбор данных отсутствует по дизайну.
