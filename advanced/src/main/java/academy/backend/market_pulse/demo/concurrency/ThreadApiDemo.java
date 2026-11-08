package academy.backend.market_pulse.demo.concurrency;

/** Runnable, run/start, конструктор и builder, join, daemon и приоритет. */
public final class ThreadApiDemo {
    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> System.out.println("Runnable выполняет " + Thread.currentThread().getName());
        Thread classic = new Thread(task, "constructor-worker");
        System.out.println("До start: " + classic.getState());
        // NOTICE: run — вызов в текущем потоке; только start создаёт отдельный поток исполнения.
        classic.run(); // обычный вызов, выводит имя main; отдельного исполнения ещё нет
        classic.start();
        classic.join();
        System.out.println("После join: " + classic.getState());
        try {
            classic.start();
        } catch (IllegalThreadStateException e) {
            System.out.println("Повторный start: " + e.getClass().getSimpleName());
        }

        int[] result = {0};
        Thread built = Thread.ofPlatform().name("builder-worker")
                .daemon(false).priority(Thread.NORM_PRIORITY).unstarted(() -> result[0] = 42);
        System.out.println("Builder unstarted: " + built.getState() + ", daemon=" + built.isDaemon()
                + ", priority=" + built.getPriority());
        built.start();
        built.join(); // HB: действия worker видны main после обнаружения завершения
        System.out.println("После join результат обычного поля: " + result[0]);
        if (result[0] != 42) {
            throw new AssertionError("join должен опубликовать результат");
        }
        System.out.println("Приоритет и yield не гарантируют порядок исполнения или долю CPU");
        // Не проверяем yield по выводу/таймингу: планировщик вправе проигнорировать подсказку.
        // Не запускаем virtual: им посвящено отдельное занятие.
    }
}
