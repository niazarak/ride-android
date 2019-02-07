# ride-android


### Описание
- Для генерации dex используется библиотека `DexMaker`.
Ограничение API - генерировать локальные переменные (=регистры) до любого кода. 
- Из типов пока только целые числа и булевы значения
- Есть базовая проверка типов на этапе компиляции

### Сборка

- Склонировать
```
https://github.com/niazarak/ride-android.git
cd ride-android
```

- Установить `gradle`, если не установлен (он скачает зависимости)

- Собрать
```
./gradlew install
```

### Запуск

- Если собирали по инструкциям выше, то:
```
build/install/ride-android/bin/ride-android example.el
```

- Либо скачать готовый jar и запустить
```
java -jar ride-android-fat-1.0.jar example.el
```

Если ничего не взорвалось, то появится файл `classes.dex`

### Запуск бинаря на телефоне

- Скачать и распаковать [девтулзы](https://developer.android.com/studio/releases/platform-tools). Нам понадобится `adb `
- Подключить телефон по USB
- Загрузить скомпилированный бинарь на телефон
```
adb push classes.dex /data/local/tmp/ride
```
- Запустить бинарь на телефоне
```
adb shell
> cd /data/local/tmp/ride/
> dalvikvm -cp classes.dex Main
```
Или в одну строку
```
adb shell 'dalvikvm -cp /data/local/tmp/ride/classes.dex Main'
```