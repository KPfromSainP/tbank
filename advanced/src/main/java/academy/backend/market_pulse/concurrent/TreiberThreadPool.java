package academy.backend.market_pulse.concurrent;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.concurrent.locks.LockSupport;

/**
 * Фиксированный набор платформенных потоков, извлекающих задачи из стека Трайбера.
 * Последняя ожидающая задача выполняется первой; ограничений размера и гарантий справедливости нет.
 */
public final class TreiberThreadPool implements AutoCloseable {
    /** Ожидающие задачи; закрытие стека одновременно запрещает дальнейшее добавление. */
    private final TreiberStack<Runnable> tasks = new TreiberStack<>();
    /** Все работники создаются до первого start; состав массива больше не меняется. */
    private final Thread[] workers;
    /** Обработчик RuntimeException отдельных задач; такая ошибка не завершает работника. */
    private final Consumer<RuntimeException> onFailure;
    /** Видимый всем работникам запрос на выход из цикла выполнения. */
    private volatile boolean stopped;

    /** Создаёт всех работников перед запуском, чтобы submit мог безопасно разбудить любого из них. */
    public TreiberThreadPool(int size, Consumer<RuntimeException> onFailure) {
        if (size <= 0) {
            throw new IllegalArgumentException("Число работников должно быть положительным");
        }
        this.onFailure = Objects.requireNonNull(onFailure);
        workers = new Thread[size];
        for (int index = 0; index < size; index++) {
            workers[index] = Thread.ofPlatform().name("quote-worker-" + (index + 1)).unstarted(this::work);
        }
        for (Thread worker : workers) {
            worker.start();
        }
    }

    /** Передаёт задачу работникам; закрытый стек отклоняет её без общей блокировки. */
    public void submit(Runnable task) {
        tasks.push(task);
        wakeWorkers();
    }

    /** Извлекает задачи до остановки пула; если стек пуст, ожидает пробуждения. */
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

    /** Приостанавливает работника; после возврата цикл снова проверит стек и признак остановки. */
    private void awaitTask() {
        // NOTICE: unpark до park сохраняет одно разрешение на продолжение.
        // Даже после неожиданного возврата из park цикл заново проверяет наличие задачи.
        LockSupport.park(this);
        Thread.interrupted();
    }

    /** Выполняет одну задачу и изолирует её ошибку и флаг прерывания от следующей задачи. */
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

    /** Отбрасывает ожидающие задачи и запрашивает прерывание всех выполняющихся задач. */
    public void shutdown() {
        tasks.close();
        stopped = true;
        for (Thread worker : workers) {
            worker.interrupt();
            LockSupport.unpark(worker);
        }
    }

    /** Дожидается всех работников; вызывающий поток не должен сам быть работником этого пула. */
    public void awaitTermination() throws InterruptedException {
        for (Thread worker : workers) {
            if (worker == Thread.currentThread()) {
                throw new IllegalStateException("Работник не может ждать сам себя");
            }
        }
        for (Thread worker : workers) {
            worker.join();
        }
    }

    /** Останавливает пул и завершает ожидание даже при прерывании, затем восстанавливает флаг. */
    @Override
    public void close() throws InterruptedException {
        shutdown();
        boolean interrupted = Thread.interrupted();
        while (true) {
            try {
                awaitTermination();
                break;
            } catch (InterruptedException error) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Ожидание остановки пула было прервано");
        }
    }
}
