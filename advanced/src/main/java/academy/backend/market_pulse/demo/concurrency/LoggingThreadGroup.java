package academy.backend.market_pulse.demo.concurrency;

/** Показывает передачу необработанного исключения от группы потоков родительскому обработчику. */
final class LoggingThreadGroup extends ThreadGroup {
    LoggingThreadGroup(String name) {
        super(name);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        System.out.println("ThreadGroup: " + thread.getName() + ", делегируем родителю");
        super.uncaughtException(thread, error);
    }
}
