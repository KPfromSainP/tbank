package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.atomic.AtomicReference;

/** finally → обработчик в падающем потоке → TERMINATED; join не перебрасывает его исключение. */
public final class UncaughtExceptionDemo {
    private static void fail() { throw new IllegalStateException("Ошибка фоновой задачи"); }

    public static void main(String[] args) throws InterruptedException {
        var observed = new AtomicReference<Throwable>();
        Thread worker = Thread.ofPlatform().name("failing-worker")
                .uncaughtExceptionHandler((thread, error) -> {
                    System.out.println("handler: " + thread.getName() + ", выполняется в "
                            + Thread.currentThread().getName() + ", ошибка: " + error);
                    observed.set(error);
                }).unstarted(() -> {
                    try { fail(); }
                    finally { System.out.println("finally: освобождение ресурсов перед handler"); }
                });
        worker.start();
        worker.join(); // исключение работника не перебрасывается в main
        System.out.println("После join: " + worker.getState() + "; main продолжает работу");
        if (!(observed.get() instanceof IllegalStateException)
                || worker.getState() != Thread.State.TERMINATED) {
            throw new AssertionError("Обработчик должен получить ошибку, работник — завершиться");
        }
        demonstrateHandlerDelegation();
    }

    /** Сравнивает делегирование группе и общему обработчику с индивидуальным обработчиком потока. */
    private static void demonstrateHandlerDelegation() throws InterruptedException {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                    System.out.println("default handler: " + thread.getName() + ": " + error));
            // Это делегирование при отсутствии индивидуального handler, не вызов всех обработчиков.
            ThreadGroup group = new LoggingThreadGroup("demo-group");
            Thread other = new Thread(group, UncaughtExceptionDemo::fail, "group-handler-worker");
            other.start();
            other.join();
            // Явный handler завершает обработку: group/default автоматически после него не вызываются.
            Thread individual = new Thread(group, UncaughtExceptionDemo::fail, "individual-handler-worker");
            individual.setUncaughtExceptionHandler((thread, error) ->
                    System.out.println("Только индивидуальный handler: " + thread.getName()));
            individual.start();
            individual.join();
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous); // настройка общая для JVM
        }
    }
}
