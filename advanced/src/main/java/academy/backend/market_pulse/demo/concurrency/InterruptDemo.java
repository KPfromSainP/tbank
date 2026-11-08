package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

/**
 * Сравнение флага при park, sleep и непрерываемом захвате монитора.
 * HTTP-примеры: {@link HttpInterruptDemo} (оба клиента или отдельный режим).
 */
public final class InterruptDemo {
    public static void main(String[] args) throws InterruptedException {
        // NOTICE: interrupt — запрос на кооперативную отмену. sleep бросает исключение и сбрасывает
        // флаг, park возвращается с флагом, захват synchronized не становится прерываемым.
        Thread sleeper = Thread.ofPlatform().name("sleep-interrupt").start(() -> {
            try { Thread.sleep(60_000); }
            catch (InterruptedException e) {
                System.out.println("sleep catch: flag=" + Thread.currentThread().isInterrupted());
                Thread.currentThread().interrupt();
            }
        });
        sleeper.interrupt(); sleeper.join();
        Thread parked = Thread.ofPlatform().name("park-interrupt").start(() -> {
            while (!Thread.currentThread().isInterrupted()) { LockSupport.park(InterruptDemo.class); }
            System.out.println("park return: flag=" + Thread.currentThread().isInterrupted());
        });
        parked.interrupt(); parked.join();
        Object lock = new Object();
        Thread blocked;
        var attempting = new CountDownLatch(1);
        synchronized (lock) {
            blocked = Thread.ofPlatform().name("monitor-interrupt").start(() -> {
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
