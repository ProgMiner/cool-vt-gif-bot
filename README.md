# cool-vt-gif-bot

Бот для генерации гифок в телеге.

## Pull request на добавление гифок

Чтобы ускорить и упростить процесс добавления гифки в бота можно создать PR,
разместив уже подготовленную (без лишнего текста) в директории [./src/main/resources/gif](./src/main/resources/gif).

Гифки хранятся в формате mp4, также, как если вы их скачиваете из интерфейса телеги.

А ещё лучше сразу создать класс фабрики в [./src/main/kotlin/ru/byprogminer/coolvtgifbot/gif/factory](./src/main/kotlin/ru/byprogminer/coolvtgifbot/gif/factory)
по примеру из других фабрик.

Для простых гифок можно отнаследоваться от [BackgroundGifFactory](./src/main/kotlin/ru/byprogminer/coolvtgifbot/gif/factory/BackgroundGifFactory.kt),
эта фабрика размещает текст в нижней трети гифки.

Для более сложных случаев стоит отнаследоваться от [AbstractGifFactory](./src/main/kotlin/ru/byprogminer/coolvtgifbot/gif/factory/AbstractGifFactory.kt)
и вызвать метод `placeText` с подходящими параметрами.

Если лень делать класс, то можно указать в комментарии к PR координаты прямоугольника, в который надо вписать текст,
если это нетривиальная гифка (не просто добавить текст внизу).

## Сборка

Для сборки нужен Maven. Сборка выполняется командой:

```bash
mvn clean package
```

## Запуск

Для запуска нужно подложить в рабочую директорию файл application.properties со следующим содержимым:

```properties
tg.host=# address of server
tg.token=# telegram bot token
```

Другие доступные параметры можно посмотреть в [application.properties](./src/main/resources/application.properties).

Либо эти параметры можно указать в аргументах командной строки в формате `--ключ=значение`.

Для запуска можно использовать команду:

```bash
java -jar ./target/cool-vt-gif-bot-0.0.1-SNAPSHOT.jar [аргументы]
```

Также по-умолчанию собранный JAR-файл поддерживает самостоятельный запуск с помощью system.d:
```
# ...

[Service]
ExecStart=/путь/до/файла/cool-vt-gif-bot.jar
SuccessExitStatus=143

# ...
```
