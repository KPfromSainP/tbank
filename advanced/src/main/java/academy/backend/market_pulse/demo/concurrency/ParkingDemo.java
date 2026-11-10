package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

/**
 * Одно разрешение на уже запущенный поток; ложные возвраты требуют повторной проверки условия.
 */
public final class ParkingDemo {
    /**
     * Разрешает worker-потоку дойти до первого park только после двух unpark.
     */
    private static volatile boolean go;
    /**
     * Условие завершения ожидания; отдельно от разрешения, которое хранит LockSupport.
     */
    private static volatile boolean released;
    public static void main(String[] args) throws InterruptedException {
        LockSupport.getBlocker(Thread.currentThread()); // закончить инициализацию до опыта
        var started = new CountDownLatch(1);
        var firstReturned = new CountDownLatch(1);
        Thread worker = Thread.ofPlatform()
                .name("parking-demo")
                .start(() -> {
            started.countDown();
            while (!go && !Thread.currentThread().isInterrupted()) { Thread.onSpinWait(); }
            if (Thread.currentThread().isInterrupted()) { return; } // без промежуточного park
            LockSupport.park(ParkingDemo.class);
            System.out.println("Первый park вернулся");
            firstReturned.countDown();
            while (!released && !Thread.currentThread().isInterrupted()) {
                LockSupport.park(ParkingDemo.class);
            }
        });
        try {
            started.await();
            // NOTICE: два unpark не накапливают два разрешения на продолжение.
            // released — проверяемое условие, а не счётчик таких разрешений.
            LockSupport.unpark(worker);
            LockSupport.unpark(worker); // остаётся не более одного разрешения
            go = true;
            firstReturned.await();
            printWaitingState(worker);
        } finally {
            go = true;
            released = true;
            worker.interrupt();
            LockSupport.unpark(worker);
            worker.join();
        }
    }

    private static void printWaitingState(Thread worker) {
        long until = System.nanoTime() + 2_000_000_000L;
        while (worker.getState() != Thread.State.WAITING && System.nanoTime() < until) {
            Thread.yield();
        }
        System.out.println("Повторное ожидание: " + worker.getState());
    }
}
