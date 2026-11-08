# Пакет adb-приёмки VLOM (фаза 5, одним заходом)

Когда устройство (Xiaomi TV A2 32 / Thomson R3 / XGIMI) доступно по adb (LAN/USB):

```bash
# 1. Подключить и прогнать всё
ADB_ADDR=<IP>:5555 tools/acceptance.sh           # release APK из app/build/outputs/apk/release/
ADB_ADDR=<IP>:5555 tools/acceptance.sh path/to/app-release.apk

# 2. Результат
#    acceptance-report-<дата>.txt   — PASS/WARN/FAIL по каждому пункту
#    acceptance-shots/<дата>/*.png  — скриншоты (home, настройки, погода, оффлайн, recovery, boot-циклы)

# 3. Пиксель-скринтюнинг (S4.1): положить эталонные скрины Vista MCE в references/
#    (имена = имена скринов: 02-home.png, 04-weather.png, ...) — скрипт сравнит сам
tools/diff_screens.py references/02-home.png acceptance-shots/<дата>/02-home.png 8
```

## Покрытие чеклиста RELEASE-CHECKLIST.md

| Пункт | Чем проверяется |
|---|---|
| Установка APK | `adb install -r` + versionName из dumpsys |
| HOME-замена | `cmd package set-home-activity` + resumedActivity |
| Холодный старт < 2 с | `am start -W TotalTime` (S5.1) |
| FPS / janky | `dumpsys gfxinfo reset` + D-pad-прогон 15 нажатий |
| Скриншоты | 8+ снимков: home, настройки, погода, оффлайн, recovery, boot×3 |
| Погода оффлайн | airplane-mode on → «Нет соединения» → off |
| Frigate | открытие → экран конфигурации |
| CrashGuard/Recovery | `am crash` ×2 → RecoveryActivity → «Продолжить» → HOME |
| Low-RAM | `dumpsys meminfo` TOTAL PSS < 250 MB |
| Boot-циклы ×3 | `adb reboot` → HOME поднимается сам → скрин |
| Скринтюнинг | `tools/diff_screens.py` против `references/` |

Примечания:
- эмуляторный x86_64 ABI поддержан (есть в APK);
- CrashGuard через `am crash` может отсутствовать на API 28 — тогда пункт = WARN (вручную);
- ориентация/координаты тапов не используются — только D-pad, как на пульте.
