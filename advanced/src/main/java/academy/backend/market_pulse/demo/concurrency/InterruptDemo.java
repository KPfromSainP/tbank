package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

/**
 * Сравнивает реакцию sleep, park и ожидания монитора на запрос прерывания.
 */
public final class InterruptDemo {
    public static void main(String[] args) throws InterruptedException {
        interruptSleep();
        interruptPark();
        interruptMonitorAcquisition();
    }

    /**
     * Показывает, что sleep бросает исключение и сбрасывает флаг, который нужно восстановить.
     */
    private static void interruptSleep() throws InterruptedException {
        Thread sleeper = Thread.ofPlatform()
                .name("sleep-interrupt")
                .start(() -> {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException error) {
                System.out.println("sleep catch: flag=" + Thread.currentThread().isInterrupted());
                Thread.currentThread().interrupt();
            }
        });
        sleeper.interrupt();
        sleeper.join();
    }

    /**
     * Показывает, что park возвращается с установленным флагом, не бросая исключения.
     */
    private static void interruptPark() throws InterruptedException {
        Thread parked = Thread.ofPlatform()
                .name("park-interrupt")
                .start(() -> {
            // NOTICE: без проверки флага повторный park сразу возвращался бы после interrupt.
            while (!Thread.currentThread().isInterrupted()) {
                LockSupport.park(InterruptDemo.class);
            }
            System.out.println("park return: flag=" + Thread.currentThread().isInterrupted());
        });
        parked.interrupt();
        parked.join();
    }

    /**
     * Показывает, что interrupt не отменяет ожидание входа в synchronized.
     */
    private static void interruptMonitorAcquisition() throws InterruptedException {
        Object lock = new Object();
        var attempting = new CountDownLatch(1);
        Thread blocked;
        synchronized (lock) {
            blocked = Thread.ofPlatform()
                    .name("monitor-interrupt")
                    .start(() -> {
                attempting.countDown();
                synchronized (lock) {
                    System.out.println("Монитор получен, флаг=" + Thread.currentThread().isInterrupted());
                }
            });
            attempting.await();
            blocked.interrupt();
            System.out.println("interrupt не освобождает монитор за другого потока");
        }
        blocked.join();
    }
}
