package academy.backend.market_pulse.demo.concurrency;

import java.util.List;
import java.util.concurrent.locks.LockSupport;

/** Оставляет потоки в наблюдаемых состояниях до Enter. getState не используется как рабочая синхронизация. */
public final class ThreadStatesDemo {
    // NOTICE: RUNNABLE в JVM включает и выполнение на CPU, и готовность к выполнению.
    // getState — диагностический снимок; порядок выполнения им не согласовывают.
    private static volatile boolean running = true;
    public static void main(String[] args) throws Exception {
        Object lock = new Object();
        Object condition = new Object();
        Thread fresh = Thread.ofPlatform().name("demo-new").unstarted(() -> {});
        Thread ended = Thread.ofPlatform().name("demo-terminated").start(() -> {});
        ended.join();
        Thread busy = Thread.ofPlatform().name("demo-runnable").unstarted(() -> {
            while (running) { Thread.onSpinWait(); }
        });
        Thread blocked = Thread.ofPlatform().name("demo-monitor").unstarted(() -> {
            synchronized (lock) { System.out.println("Монитор получен после освобождения"); }
        });
        Thread parked = Thread.ofPlatform().name("demo-parked").unstarted(() -> {
            while (running && !Thread.currentThread().isInterrupted()) { LockSupport.park(condition); }
        });
        Thread sleeper = Thread.ofPlatform().name("demo-sleeping").unstarted(() -> {
            try { Thread.sleep(60_000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        List<Thread> workers = List.of(busy, blocked, parked, sleeper);
        try {
            synchronized (lock) {
                workers.forEach(Thread::start);
                for (var entry : List.of(new Object[]{blocked, Thread.State.BLOCKED},
                        new Object[]{parked, Thread.State.WAITING},
                        new Object[]{sleeper, Thread.State.TIMED_WAITING})) {
                    Thread t = (Thread) entry[0];
                    long until = System.nanoTime() + 2_000_000_000L;
                    while (t.getState() != entry[1] && System.nanoTime() < until) { Thread.yield(); }
                }
                System.out.println("PID=" + ProcessHandle.current().pid() + "; jcmd <pid> Thread.print; Enter — выход");
                for (Thread t : List.of(fresh, busy, blocked, parked, sleeper, ended)) {
                    System.out.println(t.getName() + " = " + t.getState());
                }
                System.in.read();
            }
        } finally {
            running = false;
            workers.forEach(Thread::interrupt);
            for (Thread t : workers) { t.join(); }
        }
    }
}
