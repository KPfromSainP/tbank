package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.CountDownLatch;

/** sleep удерживает монитор; wait освобождает монитор и возвращается после его повторного захвата. */
public final class WaitingDemo {
    /** Условие продолжения waiter; все обращения выполняются под монитором опыта. */
    private static boolean ready;
    public static void main(String[] args) throws InterruptedException {
        demonstrateSleep();
        demonstrateWait();
    }

    /** Проверяет, что другой поток не может захватить монитор, пока его владелец спит. */
    private static void demonstrateSleep() throws InterruptedException {
        Object monitor = new Object();
        var entered = new CountDownLatch(1);
        Thread sleeper = Thread.ofPlatform().start(() -> {
            synchronized (monitor) {
                entered.countDown();
                try { Thread.sleep(60_000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });
        entered.await();
        Thread contender = Thread.ofPlatform().start(() -> {
            synchronized (monitor) { System.out.println("Получили монитор после выхода sleeper"); }
        });
        long until = System.nanoTime() + 2_000_000_000L;
        while (contender.getState() != Thread.State.BLOCKED && System.nanoTime() < until) { Thread.yield(); }
        System.out.println("Во время sleep претендент: " + contender.getState());
        sleeper.interrupt(); sleeper.join(); contender.join();
    }

    /** Ожидает условие через wait, освобождая монитор для потока, который устанавливает условие. */
    private static void demonstrateWait() throws InterruptedException {
        Object monitor = new Object();
        var aboutToWait = new CountDownLatch(1);
        Thread waiter = Thread.ofPlatform().start(() -> {
            synchronized (monitor) {
                // NOTICE: notifyAll не гарантирует выполнения условия; после возврата проверяем его снова.
                while (!ready) {
                    try { aboutToWait.countDown(); monitor.wait(); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
                System.out.println("Условие выполнено, монитор снова у waiter");
            }
        });
        aboutToWait.await();
        synchronized (monitor) { ready = true; monitor.notifyAll(); }
        waiter.join();
    }
}
