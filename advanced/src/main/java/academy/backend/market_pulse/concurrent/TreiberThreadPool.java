package academy.backend.market_pulse.concurrent;

import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * Фиксированный набор платформенных потоков, извлекающих задачи из стека Трайбера.
 * Последняя ожидающая задача выполняется первой; ограничений размера и гарантий справедливости нет.
 * Close вызывается владельцем после остановки всех producer; конкурентные submit и close не поддерживаются.
 */
public final class TreiberThreadPool implements AutoCloseable {
    /**
     * Стек ожидающих задач; при close оставшиеся задачи отбрасываются.
     */
    private final TreiberStack<Runnable> tasks = new TreiberStack<>();
    /**
     * Worker-потоки пула; массив заполняется до возврата из конструктора.
     */
    private final Thread[] workers;
    /**
     * Обработчик RuntimeException отдельных задач; такая ошибка не завершает worker.
     */
    private final Consumer<RuntimeException> onFailure;
    /**
     * Видимый всем worker-потокам запрос на выход из цикла выполнения.
     */
    private volatile boolean stopped;

    /**
     * Запускает worker-потоки. Задачи можно передавать только после завершения конструктора.
     */
    public TreiberThreadPool(int size, Consumer<RuntimeException> onFailure) {
        if (size <= 0) {
            throw new IllegalArgumentException("Число worker-потоков должно быть положительным");
        }
        this.onFailure = Objects.requireNonNull(onFailure);
        this.workers = new Thread[size];
        for (int index = 0; index < size; index++) {
            this.workers[index] = Thread.ofPlatform()
                    .name("quote-worker-" + (index + 1))
                    .start(this::work);
        }
    }

    /**
     * Передаёт задачу worker-потокам; после close отклоняет её.
     */
    public void submit(Runnable task) {
        if (stopped) {
            throw new IllegalStateException("Пул закрыт");
        }
        tasks.push(task);
        wakeWorkers();
    }

    /**
     * Извлекает задачи до остановки пула; если стек пуст, ожидает пробуждения.
     */
    private void work() {
        while (!stopped) {
            Runnable task = tasks.pop();
            if (task == null) {
                awaitTask();
            } else if (!stopped) {
                execute(task);
            }
        }
    }

    /**
     * Приостанавливает worker; после возврата цикл снова проверит стек и признак остановки.
     */
    private void awaitTask() {
        // NOTICE: unpark до park сохраняет одно разрешение на продолжение.
        // Даже после неожиданного возврата из park цикл заново проверяет наличие задачи.
        LockSupport.park(this);
        Thread.interrupted();
    }

    /**
     * Выполняет одну задачу и изолирует её ошибку и флаг прерывания от следующей задачи.
     */
    private void execute(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException error) {
            reportFailure(error);
        } finally {
            // NOTICE: отмена относится к текущей задаче. Оставленный флаг заставил бы
            // следующий park немедленно возвращаться и загрузил бы процессор пустым циклом.
            Thread.interrupted();
        }
    }

    private void reportFailure(RuntimeException error) {
        try {
            onFailure.accept(error);
        } catch (RuntimeException reportingError) {
            System.err.println("Ошибка обработчика задачи: " + reportingError);
        }
    }

    private void wakeWorkers() {
        for (Thread worker : workers) {
            LockSupport.unpark(worker);
        }
    }

    /**
     * Отбрасывает ожидающие задачи, прерывает worker-потоки и дожидается их завершения.
     * Вызывается владельцем пула, вне его worker-потоков.
     */
    @Override
    public void close() throws InterruptedException {
        stopped = true;
        tasks.clear();
        for (Thread worker : workers) {
            // interrupt также пробуждает worker из park; отдельный unpark не нужен.
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
