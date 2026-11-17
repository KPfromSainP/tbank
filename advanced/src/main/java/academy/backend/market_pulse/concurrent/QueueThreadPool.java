package academy.backend.market_pulse.concurrent;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Промежуточный этап рефакторинга пула с неограниченной FIFO-очередью.
 * Producer останавливается владельцем перед close, как у TreiberThreadPool.
 */
public final class QueueThreadPool implements TaskPool {
    /**
     * Очередь передаёт задачи и обеспечивает ожидание consumer через take.
     */
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    /**
     * Worker-потоки переиспользуются между задачами.
     */
    private final Thread[] workers;
    /**
     * Обработчик RuntimeException задачи или callback.
     */
    private final Consumer<RuntimeException> onFailure;
    /**
     * Close публикует остановку всем worker-потокам.
     */
    private volatile boolean stopped;

    public QueueThreadPool(int size, Consumer<RuntimeException> onFailure) {
        if (size <= 0) {
            throw new IllegalArgumentException("Число worker-потоков должно быть положительным");
        }
        this.onFailure = Objects.requireNonNull(onFailure);
        this.workers = new Thread[size];
        for (int index = 0; index < size; index++) {
            this.workers[index] = Thread.ofPlatform()
                    .name("queue-worker-" + (index + 1))
                    .start(this::work);
        }
    }

    @Override
    public void submit(Runnable task) {
        if (stopped) {
            throw new IllegalStateException("Пул закрыт");
        }
        tasks.add(task);
    }

    /**
     * take ожидает задачу, а interrupt во время ожидания завершает worker.
     */
    private void work() {
        while (!stopped && !Thread.currentThread().isInterrupted()) {
            try {
                Runnable task = tasks.take();
                if (!stopped) {
                    execute(task);
                }
            } catch (InterruptedException error) {
                // NOTICE: take сбрасывает флаг через исключение. Восстанавливаем его для выхода из while.
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Изолирует ошибку и interrupt одной задачи от следующего запроса.
     */
    private void execute(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException error) {
            try {
                onFailure.accept(error);
            } catch (RuntimeException handlerError) {
                System.err.println("Ошибка обработчика задачи: " + handlerError);
            }
        } finally {
            // NOTICE: отменена текущая задача. Состояние остановки пула хранится отдельно в stopped.
            Thread.interrupted();
        }
    }

    @Override
    public void close() throws InterruptedException {
        stopped = true;
        tasks.clear();
        for (Thread worker : workers) {
            worker.interrupt();
        }
        boolean interrupted = Thread.interrupted();
        for (Thread worker : workers) {
            while (worker.isAlive()) {
                try {
                    worker.join();
                } catch (InterruptedException error) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Ожидание остановки пула было прервано");
        }
    }
}
