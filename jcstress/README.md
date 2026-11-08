# JMM-эксперименты семинара 10

Модуль общего Maven-проекта. Это **jcstress, не JUnit и не JMH**. Импортируйте корневой
`pom.xml` в IDEA: приложение и эксперименты будут видны вместе. Пакет опытов —
`academy.backend.market_pulse.jmm`; обычная сборка не запускает конкурентные эксперименты.

Из корня репозитория:

```bash
mvn -pl jcstress -am package -DskipTests
java -jar jcstress/target/jcstress.jar -l -t 'academy.backend.market_pulse.jmm.*'
```

Запуск, разрешённые исходы и порядок демонстрации:
[Эксперименты JMM](../advanced/docs/materials/seminar10/Эксперименты%20JMM.md).
Сгенерированные результаты и target в Git не храним. Имена опытов и их семантика при
форматировании сохранены, поэтому прежние фильтры запуска остаются применимы.
