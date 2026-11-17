# ConcurrentMapBenchmark: сохранённые результаты

Сохранённый прогон исходных материалов семинара 11.

Apple M2; macOS 26.5.2 arm64; JBR/OpenJDK 25.0.3; JMH 1.37.
2 forks; warmup 3 x 1 s; measurement 5 x 1 s; 4096 ключей.
Это локальный пример, а не универсальный рейтинг реализаций.

## Только чтение

| Map | Потоки | Млн ops/s | Погрешность JMH |
|---|---:|---:|---:|
| hashMap | 1 | 484.80 | +/- 1.67 |
| synchronizedMap | 1 | 177.11 | +/- 1.08 |
| concurrentHashMap | 1 | 392.12 | +/- 4.79 |
| hashMap | 8 | 2154.95 | +/- 142.41 |
| synchronizedMap | 8 | 48.39 | +/- 6.51 |
| concurrentHashMap | 8 | 1862.91 | +/- 63.16 |

HashMap участвует только в чтении после заполнения в @Setup.

## Смешанная нагрузка: 3 читателя + 1 писатель

| Map | Ключи | Всего, млн ops/s | Чтение | Запись |
|---|---|---:|---:|---:|
| synchronizedMap | Распределённые | 35.34 | 25.05 | 10.29 |
| concurrentHashMap | Распределённые | 1067.21 | 973.57 | 93.64 |
| synchronizedMap | Один горячий | 37.06 | 27.72 | 9.34 |
| concurrentHashMap | Один горячий | 552.13 | 529.36 | 22.76 |

Писатель выполняет put существующего значения по существующему ключу. Тест не измеряет рост Map и вставку новых ключей.
Ключи обходятся детерминированно с разными начальными сдвигами. Высокая сумма операций не означает высокую скорость писателя.

## Как повторить

Запустить org.openjdk.jmh.Main с classpath собранного advanced и зависимостей JMH:

```text
ConcurrentMapBenchmark.read -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s
ConcurrentMapBenchmark.read -t 8 -f 2 -wi 3 -i 5 -w 1s -r 1s
ConcurrentMapBenchmark.mixed -t 4 -f 2 -wi 3 -i 5 -w 1s -r 1s
```

Использован существующий ConcurrentMapBenchmark. Для другой машины прогон нужно повторить.
