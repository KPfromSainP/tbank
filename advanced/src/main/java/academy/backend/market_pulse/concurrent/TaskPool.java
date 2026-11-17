package academy.backend.market_pulse.concurrent;

/**
 * Передача задач собственным worker-потокам через TreiberStack или BlockingQueue.
 * Владелец останавливает producer перед close и вызывает close вне worker-потоков.
 */
public interface TaskPool extends AutoCloseable {
    void submit(Runnable task);

    @Override
    void close() throws InterruptedException;
}
