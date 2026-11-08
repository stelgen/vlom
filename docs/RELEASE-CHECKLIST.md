# Релиз-чеклист VLOM (фаза 5 / S5.3)

Перед выставлением тега `vX.Y.Z`:

## Код
- [ ] `./gradlew assembleDebug testDebugUnitTest assembleRelease` — зелёные локально
- [ ] CI на push — зелёный (build + unit + gitleaks)
- [ ] `versionName`/`Version.current` подняты, `versionCode` увеличен
- [ ] Коммит тега **не содержит** `[skip ci]` нигде в сообщении (иначе Release не стартует)
- [ ] gitleaks локально перед пушем: секретов нет

## Проверка (при наличии устройства/adb)
- [ ] APK ставится на Xiaomi TV A2 32 / Thomson R3 / XGIMI
- [ ] VLOM назначен HOME, кнопка Home открывает старт-скрин
- [ ] D-pad: навигация по полосе, запуск приложений, тайтлы Погода/Наблюдение
- [ ] Настройки: добавление APK, добавление сайта, скрытие/сортировка, тема
- [ ] CrashGuard: 2 искусственных падения → Recovery-экран → «Открыть стоковый лаунчер»
- [ ] Cold start < 2 с (StartupTimer в логкате), 60 FPS анимации полосы
- [ ] Boot-цикл: перезагрузка ×3 — HOME поднимается сам, не падает
- [ ] Отключение сети: Погода показывает «Нет соединения», HOME работает

## Релиз
- [ ] `git tag vX.Y.Z <commit>` + `git push origin vX.Y.Z`
- [ ] Release workflow собрал APK и создал Release с автонотами
- [ ] APK в Release ставится и подписан (проверить установку)
