# Команды CLI

Запуск: `java -cp <classpath> academy.backend.market_pulse.Main <команда> [аргументы]`.

Стартовые данные (создаются при каждом запуске заново, ничего не сохраняется между запусками,
кроме `save` в файл): `SBER` (акция, RUB), `OFZ26233` (облигация, RUB), `FXUS` (ETF, USD).

## add

Добавить инструмент в репозиторий.

```
add <TYPE> <TICKER> <NAME> <CURRENCY>
```

- `TYPE` — `STOCK`, `BOND` или `ETF`
- `CURRENCY` — `RUB`, `USD` или `EUR`

```bash
add STOCK LKOH Лукойл RUB
```

```
Добавлено: Акция, сектор: Unspecified
```

Повторное добавление уже существующего тикера — ошибка (делистинг не выполняется автоматически).

## list

Показать инструменты с опциональной фильтрацией и сортировкой.

```
list [--type TYPE] [--ticker SUBSTRING] [--currency CURRENCY]
     [--price-op GE|LE|EQ --price VALUE] [--sort-by TICKER|CURRENCY|PRICE] [--order ASC|DESC]
```

```bash
list
list --type STOCK
list --currency RUB --sort-by TICKER
list --price-op GE --price 6.0 --sort-by PRICE --order DESC
```

```
Акция, сектор: Финансы
```

`--price-op`/`--price` фильтруют только акции — по дивидендной доходности (`Stock.getDividendYield()`);
облигации и ETF под этот фильтр не попадают никогда, независимо от значения.

## watch

Добавить тикер в watchlist или показать текущий список (без аргумента).

```
watch [TICKER]
```

```bash
watch SBER
watch
```

```
Добавлено в watchlist: SBER
Watchlist: [SBER]
```

## quote

Получить котировки по одному или нескольким тикерам (из `QuoteSource` — заглушки без реальной сети).

```
quote <TICKER> [TICKER...]
```

```bash
quote SBER GAZP
```

```
SBER: 250.00 RUB ▲ 1.20%
GAZP: 120.50 RUB ▼ 0.80%
```

Котировка есть для `SBER`, `GAZP`, `LKOH`, `AAPL`, `OFZ26233`, `FXUS` (заготовлены в
`AppConfig.sampleQuotes()`); для остальных тикеров — `Котировка не найдена: <TICKER>`.

## remove

Делистинг: точечно по тикеру либо массово по валюте. После удаления из watchlist автоматически
прибираются «висячие» тикеры удалённых инструментов (актуально только в рамках одного запуска
`Main`, где `repository` и `watchlist` общие — каждый вызов CLI из командной строки поднимает их
заново, поэтому между отдельными запусками watchlist пуст).

```
remove <TICKER>
remove --currency CURRENCY
```

```bash
remove SBER
```

```
Удалено: SBER
```

```bash
remove --currency RUB
```

```
Удалено (RUB): [SBER, OFZ26233]
```

Без тикера и без `--currency` — ошибка («Укажите тикер или --currency»).

## save

Сохранить текущий список инструментов в файл (одна строка на инструмент, `toString()`).

```
save <PATH>
```

```bash
save instruments.txt
```

```
Сохранено инструментов: 3 (instruments.txt)
```

## stats

Печатает все зарегистрированные статистики — каждая строка: код статистики и её результат над шестью
котировками `AppConfig.sampleQuotes()`.

```
stats
```

```bash
stats
```

```
COUNT_BY_TYPE: {ETF=1, BOND=1, STOCK=4}
COUNT_BY_CURRENCY: {USD=2, RUB=4}
AVERAGE_CHANGE: 0.6583333333333333
TOTAL_PRICE: 8482.65
GAIN_LOSS_SPLIT: {false=2, true=4}
```

Новая статистика добавляется без изменения `StatsCommand`: класс в `stat.impl` + значение в `StatCode` + строка в
`AppConfig.stats()`.

## compare

Сравнить доходность инструментов по тикерам — по убыванию `changePercent`; тикеры, которых нет в
`QuoteSource`, молча пропускаются.

```
compare <TICKER> [TICKER...]
```

```bash
compare SBER GAZP LKOH
```

```
LKOH: 7100.00 RUB ▲ 2.30%
SBER: 250.00 RUB ▲ 1.20%
GAZP: 120.50 RUB ▼ 0.80%
```

## movers

Топ инструментов по изменению цены среди всех котировок `QuoteSource` — по умолчанию растущие,
`--losers` — упавшие.

```
movers [--top N] [--losers]
```

```bash
movers --top 3
movers --top 2 --losers
```

```
LKOH: 7100.00 RUB ▲ 2.30%
SBER: 250.00 RUB ▲ 1.20%
FXUS: 6.85 USD ▲ 0.90%
```

## history

Скользящее среднее по истории цен инструмента — через Gatherer API (`MovingAverageGatherer`). История
цен — заглушка-`Map` в `AppConfig.samplePriceHistory()`, есть только для `SBER` и `LKOH`.

```
history <TICKER> [--sma K]
```

```bash
history SBER --sma 3
```

```
Цены: [244.00, 246.50, 245.00, 248.00, 250.00, 249.50, 251.20]
SMA(3): [245.17, 246.50, 247.67, 249.17, 250.23]
```

## update

Забирает актуальные котировки у `instruments-service` через `TInvestApiClient` и печатает их. Работает
против мокового сервера в Docker (`docker/mockserver`, порт 1080) — поднять заранее:

```bash
docker compose -f docker/mockserver/docker-compose.yml up -d
```

```
update
```

Реализацию клиента выбирает переменная окружения `MARKET_PULSE_CLIENT`: `URL_CONNECTION` (блокирующий
`HttpURLConnection`), `REACTOR` (самописный на `Selector`) или `HTTP_CLIENT` (по умолчанию, готовый `HttpClient`).
Сбой на стороне мока включает `MARKET_PULSE_MOCK_SCENARIO=error|timeout`.

```bash
MARKET_PULSE_CLIENT=URL_CONNECTION java -cp <classpath> academy.backend.market_pulse.Main update
```

```
SBER: 250.00 RUB ▲ 1.20%
GAZP: 120.50 RUB ▼ 0.80%
Получено котировок: 2
```


## subscriptions

Интерактивные временные подписки на котировки. Заранее поднять моковый сервер командой
из раздела `update`. Из корня репозитория:

```bash
MARKET_PULSE_CLIENT=HTTP_CLIENT java -cp <classpath> academy.backend.market_pulse.Main subscriptions --workers 3
```

`<classpath>` — classpath приложения с зависимостями, как при запуске остальных CLI-команд.
В IDEA: main-класс `academy.backend.market_pulse.Main`, аргументы `subscriptions --workers 3`,
переменная окружения `MARKET_PULSE_CLIENT=HTTP_CLIENT`.
Команда требует контракт `InterruptibleTInvestApiClient`: параллельные запросы и прерываемое ожидание.
Из трёх учебных реализаций его поддерживает `HTTP_CLIENT`; при `URL_CONNECTION` или `REACTOR`
команда сообщает об ошибке до начала сессии. Другая реализация с тем же контрактом тоже допустима.
`--workers` — положительное число платформенных работников, по умолчанию `3`.

После запуска вводить в консоли процесса:

```text
watch SBER 3s 30s
watch GAZP 1s 10s
remove SBER
exit
```

| Команда внутри `subscriptions` | Действие |
|-------------------------------|----------|
| `watch TICKER INTERVAL LIFETIME` | Создать подписку: первый запрос сразу, затем интервал после завершения предыдущего запроса |
| `remove TICKER` | Удалить подписку и прервать выполняющийся запрос |
| `exit` или EOF | Остановить диспетчер и пул, прервать активные задачи и дождаться завершения потоков |

Длительности: положительное целое число с единицей `ms`, `s`, `m` или `h`; интервал и срок
не больше 7 дней. Тикер нормализуется к верхнему регистру, повторная активная подписка — ошибка.
По истечении срока новые запросы не запускаются; уже выполняющийся запрос может завершиться.
Если истёкшая задача ещё ждёт worker в стеке, тикер можно зарегистрировать заново;
старая задача будет пропущена при извлечении. Выполняющийся запрос удерживает тикер до завершения.
Каждый запрос загружает каталог целиком и выбирает котировку по тикеру, результат печатается
как `[SBER] 250.00`. Пул использует LIFO, поэтому порядок обработки разных подписок не гарантирован.

Сервис передаёт обработчику `SubscriptionResult.PriceUpdate` или `SubscriptionResult.Failure`.
Цена остаётся числом, ошибка сохраняет исходное исключение; текст формирует CLI.
Сбой самого обработчика передаётся отдельно через `onTaskFailure`, чтобы не вызывать его повторно.

`HTTP_CLIENT` использует `send`, который реагирует на interrupt. Сценарий задержки мока:

```bash
MARKET_PULSE_CLIENT=HTTP_CLIENT MARKET_PULSE_MOCK_SCENARIO=timeout java -cp <classpath> academy.backend.market_pulse.Main subscriptions
```

В этом режиме `watch` и `remove` — команды интерактивной сессии подписок.
Верхнеуровневые CLI-команды `watch` и `remove` работают с watchlist и репозиторием.

## Демонстрации потоков

Запускать main-классы из пакета `academy.backend.market_pulse.demo.concurrency` в IDEA
или из корня репозитория после `mvn -pl advanced -am compile`:

```bash
java -cp advanced/target/classes academy.backend.market_pulse.demo.concurrency.ThreadApiDemo
```

| Класс | Что показывает |
|-------|----------------|
| `ThreadApiDemo` | `run`/`start`, builder платформенного потока, `join`, daemon и приоритет |
| `ThreadStatesDemo` | Состояния потоков; до Enter можно выполнить `jcmd <pid> Thread.print` |
| `WaitingDemo` | `sleep` удерживает монитор, `wait` освобождает его |
| `ParkingDemo` | `park`/`unpark`, один permit и повторная проверка условия |
| `InterruptDemo` | Прерывание `sleep`, `park` и ожидания монитора |
| `BlockingIoDemo` | Блокирующее чтение сокета; Enter закрывает ресурс и завершает ожидание |
| `HttpInterruptDemo` | Отмена `HttpClient.send` и поведение `HttpURLConnection`; аргументы `both` (по умолчанию), `http-client`, `url-connection` |
| `UncaughtExceptionDemo` | Обработка необработанного исключения фонового потока |
| `AccountRaceDemo` | Проверка баланса вместе с изменением и отдельно от него |
| `PublicationDemo` | Публикация обычного поля через volatile-флаг |

## jcstress

Конкурентные эксперименты запускаются отдельно от CLI и JUnit. Из корня репозитория:

```bash
mvn -pl jcstress -am package -DskipTests
cd jcstress
java -jar target/jcstress.jar -l -t 'academy.backend.market_pulse.jmm.*'
java -jar target/jcstress.jar -t 'academy.backend.market_pulse.jmm.PublicationVolatile' -m quick -f 1 -iters 2 -time 100 -c 2 -hs 128 -af NONE -jvmArgs '-Xms128m -Xmx128m' -r results
```

Для всех опытов заменить фильтр `-t` на `'academy.backend.market_pulse.jmm.*'`.
Отчёт — `jcstress/results/index.html`. Пример опыта, интерпретация отчёта и результаты:
[Эксперименты JMM](materials/seminar10/Эксперименты%20JMM.md).
