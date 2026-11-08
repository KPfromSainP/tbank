package academy.backend.market_pulse.concurrent;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.concurrent.locks.LockSupport;

/** Учебный фиксированный пул: LIFO, без Future, лимита очереди и гарантий справедливости. */
public final class TreiberThreadPool implements AutoCloseable {
    private final TreiberStack<Runnable> tasks = new TreiberStack<>();
    private final Object lifecycle = new Object();
    private final Thread[] workers;
    private final Consumer<RuntimeException> onFailure;
    // NOTICE: volatile публикует остановку всем worker, но не делает составные действия атомарными.
    private volatile boolean stopped;

    public TreiberThreadPool(int size, Consumer<RuntimeException> onFailure) {
        if (size <= 0) {
            throw new IllegalArgumentException("Число работников должно быть положительным");
        }
        this.onFailure = Objects.requireNonNull(onFailure);
        workers = new Thread[size];
        for (int i = 0; i < size; i++) {
            workers[i] = Thread.ofPlatform().name("quote-worker-" + (i + 1)).unstarted(this::work);
        }
        // Массив готов до запуска: submit может разбудить любого уже созданного worker.
        for (Thread worker : workers) {
            worker.start();
        }
    }

    public void submit(Runnable task) {
        Objects.requireNonNull(task);
        // NOTICE: один монитор согласует submit с shutdown: после закрытия новая задача не попадёт в стек.
        synchronized (lifecycle) {
            if (stopped) {
                throw new IllegalStateException("Пул закрыт");
            }
            tasks.push(task);
            // Учебное упрощение: будим всех. Permit сохраняет пробуждение до следующего park.
            for (Thread worker : workers) {
                LockSupport.unpark(worker);
            }
        }
    }

    private void work() {
        while (!stopped) {
            Runnable task = tasks.pop();
            if (task == null) {
                // NOTICE: park может вернуться без задачи. Условие всегда перепроверяется в цикле;
                // unpark до park сохраняет один permit и предотвращает потерянное пробуждение.
                LockSupport.park(this);
                // Прерывание задачи не должно превратить последующий park в busy-loop.
                Thread.interrupted();
            } else if (!stopped) {
                try {
                    // NOTICE: RuntimeException задачи не убивает worker. Error намеренно не перехватываем.
                    task.run();
                } catch (RuntimeException error) {
                    try {
                        onFailure.accept(error);
                    } catch (RuntimeException reportingError) {
                        System.err.println("Ошибка обработчика задачи: " + reportingError);
                    }
                } finally {
                    Thread.interrupted();
                }
            }
        }
    }

    /** Немедленная учебная остановка: ожидающие задачи отбрасываются, работники прерываются. */
    public void shutdown() {
        synchronized (lifecycle) {
            stopped = true;
            tasks.clear();
            for (Thread worker : workers) {
                worker.interrupt();
                LockSupport.unpark(worker);
            }
        }
    }

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

    @Override
    public void close() throws InterruptedException {
        shutdown();
        awaitTermination();
    }
}
