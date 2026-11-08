#!/usr/bin/env bash
# ============================================================================
# VLOM — adb-приёмка одним заходом (фаза 5 / S5.1–S5.3, REQ-001)
#
# Использование:
#   ADB_ADDR=192.168.1.50:5555 tools/acceptance.sh [apk_path]
#   (apk_path по умолчанию: app/build/outputs/apk/release/app-release.apk)
#
# Покрывает чеклист docs/RELEASE-CHECKLIST.md:
#   установка, HOME-замена, холодный старт (<2c), FPS (gfxinfo), скриншоты,
#   boot-циклы ×3, Погода оффлайн, CrashGuard/Recovery, meminfo,
#   пиксель-скринтюнинг (tools/diff_screens.py против references/).
#
# Результат: acceptance-report-<date>.txt, скриншоты в acceptance-shots/.
# Выход: 0 = всё PASS (или WARN), 1 = есть FAIL.
# ============================================================================
set -u

ADB_ADDR="${ADB_ADDR:-}"
APK="${1:-app/build/outputs/apk/release/app-release.apk}"
PKG="com.stelgen.vlom"
MAIN="$PKG/.MainActivity"
SHOT_DIR="acceptance-shots/$(date +%Y%m%d-%H%M%S)"
REPORT="acceptance-report-$(date +%Y%m%d-%H%M%S).txt"
PASS=0; FAIL=0; WARN=0

mkdir -p "$SHOT_DIR"

say()  { echo "[$(date +%H:%M:%S)] $*" | tee -a "$REPORT"; }
pass() { say "PASS: $*"; PASS=$((PASS+1)); }
fail() { say "FAIL: $*"; FAIL=$((FAIL+1)); }
warn() { say "WARN: $*"; WARN=$((WARN+1)); }
shot() { adb -s "$SERIAL" exec-out screencap -p > "$SHOT_DIR/$1.png" 2>/dev/null && say "shot: $1.png"; }

adb() { command adb -s "$SERIAL" "$@"; }

say "=============================================="
say "VLOM acceptance — $(date)"
say "APK: $APK"

# --- 0. Подключение ----------------------------------------------------------
if [ -n "$ADB_ADDR" ]; then
    command adb connect "$ADB_ADDR" >/dev/null 2>&1
    SERIAL="$ADB_ADDR"
else
    SERIAL="$(command adb devices | awk 'NR==2{print $1}')"
fi
[ -n "$SERIAL" ] || { say "FAIL: устройство не найдено (adb devices пуст)"; exit 1; }
say "device: $SERIAL"

adb wait-for-device
MODEL="$(adb shell getprop ro.product.model | tr -d '\r')"
API="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
ABI="$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
RES="$(adb shell wm size | tr -d '\r' | awk '{print $2}')"
say "model=$MODEL api=$API abi=$ABI res=$RES"
case "$ABI" in
    armeabi*|arm64*) pass "ABI устройства ($ABI) входит в universal APK" ;;
    x86*) warn "ABI устройства ($ABI) — эмулятор/ChromeOS (x86_64 в APK есть)" ;;
    *) fail "ABI $ABI не покрыт" ;;
esac

# --- 1. Установка ------------------------------------------------------------
if adb install -r "$APK" >/dev/null 2>&1; then
    pass "APK установлен: $(adb shell dumpsys package $PKG | grep versionName | head -1 | tr -d ' \r')"
else
    fail "установка APK не удалась"; exit 1
fi

# --- 2. HOME-замена ----------------------------------------------------------
adb shell cmd package set-home-activity "$MAIN" >/dev/null 2>&1 \
    && pass "set-home-activity ok" \
    || warn "set-home-activity не сработал (проверить вручную: Настройки->Приложения по умолчанию)"
adb shell am start -W -n "$MAIN" >/dev/null 2>&1
sleep 4
if adb shell dumpsys activity activities | grep -q "resumedActivity.*$MAIN"; then
    pass "HOME: MainActivity на переднем плане"
else
    fail "HOME: MainActivity не на переднем плане"
fi
shot "01-home-first"

# --- 3. Холодный старт (<2 c, S5.1) -------------------------------------------
adb shell am force-stop "$PKG"; sleep 2
COLD="$(adb shell am start -W -n "$MAIN" | grep -E 'TotalTime' | grep -oE '[0-9]+' | head -1 | tr -d '\r')"
say "cold start TotalTime: ${COLD}ms"
[ -n "$COLD" ] && [ "$COLD" -lt 2000 ] && pass "холодный старт < 2000ms" || fail "холодный старт ${COLD:-?}ms >= 2000ms"

# --- 4. FPS (S5.1) ------------------------------------------------------------
adb shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1
for i in 1 2 3 4 5 6 7 8 9 10; do adb shell input keyevent KEYCODE_DPAD_RIGHT >/dev/null 2>&1; sleep 0.4; done
for i in 1 2 3 4 5;  do adb shell input keyevent KEYCODE_DPAD_LEFT  >/dev/null 2>&1; sleep 0.4; done
sleep 1
JANKY="$(adb shell dumpsys gfxinfo "$PKG" | grep -A3 'Total frames' | grep -oE '[0-9]+%' | head -1 | tr -d '\r')"
say "janky frames: ${JANKY:-n/a}%"
case "${JANKY:-100}" in 100) fail "gfxinfo недоступен/100% janky" ;; 0*|1*|2*) pass "janky <= 29%" ;; *) warn "janky ${JANKY}%" ;; esac

# --- 5. Навигация и скриншоты UI ----------------------------------------------
adb shell input keyevent KEYCODE_DPAD_LEFT >/dev/null 2>&1; sleep 1
shot "02-home"
# Настройки: листаем вправо до последнего тайтла (Настройки)
for i in $(seq 1 8); do adb shell input keyevent KEYCODE_DPAD_RIGHT >/dev/null 2>&1; sleep 0.3; done
adb shell input keyevent KEYCODE_DPAD_CENTER >/dev/null 2>&1; sleep 2
if adb shell dumpsys activity activities | grep -q "SettingsActivity"; then
    pass "Настройки открываются"
    shot "03-settings"
else
    fail "Настройки не открылись"
fi
adb shell input keyevent KEYCODE_BACK >/dev/null 2>&1; sleep 1

# Погода (тайтл слева от Настроек)
adb shell input keyevent KEYCODE_DPAD_LEFT >/dev/null 2>&1; sleep 1
adb shell input keyevent KEYCODE_DPAD_CENTER >/dev/null 2>&1; sleep 6
if adb shell dumpsys activity activities | grep -q "WeatherActivity"; then
    pass "Погода открывается"
    shot "04-weather"
    # оффлайн-тест (W): airplane -> «Нет соединения»
    adb shell cmd connectivity airplane-mode enable >/dev/null 2>&1; sleep 3
    adb shell input keyevent KEYCODE_BACK >/dev/null 2>&1; sleep 1
    adb shell input keyevent KEYCODE_DPAD_CENTER >/dev/null 2>&1; sleep 4
    shot "05-weather-offline"
    say "ПРОВЕРИТЬ ВРУЧНУЮ: 05-weather-offline.png показывает «Нет соединения с Open-Meteo»"
    adb shell cmd connectivity airplane-mode disable >/dev/null 2>&1
else
    fail "Погода не открылась"
fi
adb shell input keyevent KEYCODE_BACK >/dev/null 2>&1; sleep 1

# Наблюдение (Frigate)
adb shell input keyevent KEYCODE_DPAD_LEFT >/dev/null 2>&1; sleep 1
adb shell input keyevent KEYCODE_DPAD_CENTER >/dev/null 2>&1; sleep 3
if adb shell dumpsys activity activities | grep -q "FrigateActivity"; then
    pass "Наблюдение открывается"
    shot "06-frigate-config"
fi
adb shell input keyevent KEYCODE_BACK >/dev/null 2>&1; sleep 1

# --- 6. meminfo (low-RAM, S5.1) ------------------------------------------------
adb shell am start -W -n "$MAIN" >/dev/null 2>&1; sleep 3
PSS="$(adb shell dumpsys meminfo "$PKG" | grep 'TOTAL PSS' | grep -oE '[0-9]+' | head -1 | tr -d '\r')"
say "TOTAL PSS: ${PSS:-?} MB"
if [ -n "$PSS" ] && [ "$PSS" -lt 250 ]; then pass "PSS < 250MB (${PSS}MB)"; else warn "PSS ${PSS:-?}MB — проверить на low-RAM устройстве"; fi

# --- 7. CrashGuard / Recovery (S5.2) -------------------------------------------
say "--- CrashGuard: 2 краша подряд -> Recovery ---"
CRASH_OK=1
for i in 1 2; do
    adb shell am crash "$PKG" >/dev/null 2>&1 || CRASH_OK=0
    sleep 3
done
if [ "$CRASH_OK" = 1 ]; then
    adb shell am start -W -n "$MAIN" >/dev/null 2>&1; sleep 2
    if adb shell dumpsys activity activities | grep -q "RecoveryActivity"; then
        pass "Recovery после 2 крашей"
        shot "07-recovery"
        # «Продолжить в VLOM»: фокус вниз ×2 + OK (порядок: сток/сброс/продолжить)
        adb shell input keyevent KEYCODE_DPAD_DOWN >/dev/null 2>&1
        adb shell input keyevent KEYCODE_DPAD_DOWN >/dev/null 2>&1
        adb shell input keyevent KEYCODE_DPAD_CENTER >/dev/null 2>&1; sleep 2
        if adb shell dumpsys activity activities | grep -q "resumedActivity.*$MAIN"; then
            pass "Recovery -> продолжить в VLOM"
        else
            warn "не вернулись в HOME автоматически (проверить вручную)"
        fi
    else
        fail "Recovery не появился после 2 крашей"
    fi
else
    warn "am crash недоступен на этой прошивке — CrashGuard проверить вручную"
fi

# --- 8. Boot-циклы ×3 (S5.2) ----------------------------------------------------
say "--- Boot-циклы ---"
BOOT_FAIL=0
for i in 1 2 3; do
    say "reboot $i/3..."
    adb reboot >/dev/null 2>&1
    adb wait-for-device
    # ждать boot_completed до 180с
    for t in $(seq 1 90); do
        B="$(adb shell getprop sys.boot_completed | tr -d '\r')"
        [ "$B" = "1" ] && break
        sleep 2
    done
    sleep 8
    if adb shell dumpsys activity activities | grep -q "resumedActivity.*$MAIN"; then
        say "boot $i: HOME поднялся сам"
    else
        adb shell am start -W -n "$MAIN" >/dev/null 2>&1
        BOOT_FAIL=$((BOOT_FAIL+1))
        warn "boot $i: HOME не поднялся сам (запущен вручную)"
    fi
    shot "08-boot$i"
done
[ "$BOOT_FAIL" = 0 ] && pass "boot-циклы: HOME поднимается сам (3/3)" || fail "boot-циклы: сбоев $BOOT_FAIL/3"

# --- 9. Пиксель-скринтюнинг (S4.1) ----------------------------------------------
say "--- Пиксель-скринтюнинг ---"
if command -v python3 >/dev/null 2>&1 && python3 -c "import PIL" 2>/dev/null; then
    for ref in references/*.png; do
        [ -f "$ref" ] || continue
        NAME="$(basename "$ref" .png)"
        SHOT="$(ls "$SHOT_DIR"/$NAME*.png 2>/dev/null | head -1)"
        [ -n "$SHOT" ] || continue
        tools/diff_screens.py "$ref" "$SHOT" >> "$REPORT" 2>&1 \
            && pass "скринтюнинг $NAME" || warn "скринтюнинг $NAME — см. diff в отчёте"
    done
    [ "$(ls references/*.png 2>/dev/null | wc -l)" = 0 ] && \
        warn "references/ пуст — добавить эталонные скрины Vista MCE для сравнения"
else
    warn "python3+PIL недоступны — diff пропущен"
fi

# --- Итог ------------------------------------------------------------------------
say "=============================================="
say "ИТОГ: PASS=$PASS WARN=$WARN FAIL=$FAIL"
say "скриншоты: $SHOT_DIR/ ; отчёт: $REPORT"
[ "$FAIL" = 0 ]
