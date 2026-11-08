package academy.backend.market_pulse.demo.concurrency;

/** Сравнивает прямой вызов run с запуском потока и показывает публикацию результата через join. */
public final class ThreadApiDemo {
    public static void main(String[] args) throws InterruptedException {
        demonstrateRunAndStart();
        demonstrateBuilderAndJoin();
        System.out.println("Приоритет и yield не гарантируют порядок исполнения или долю процессора");
    }

    /** Показывает, в каком потоке выполняется run и почему start нельзя вызвать повторно. */
    private static void demonstrateRunAndStart() throws InterruptedException {
        Runnable task = () -> System.out.println("Runnable выполняет " + Thread.currentThread().getName());
        Thread classic = new Thread(task, "constructor-worker");
        System.out.println("До start: " + classic.getState());
        // NOTICE: run — обычный вызов в текущем потоке. Отдельное выполнение создаёт start.
        classic.run();
        classic.start();
        classic.join();
        System.out.println("После join: " + classic.getState());
        try {
            classic.start();
        } catch (IllegalThreadStateException error) {
            System.out.println("Повторный start: " + error.getClass().getSimpleName());
        }
    }

    /** Настраивает поток через builder и читает обычное поле после обнаружения его завершения. */
    private static void demonstrateBuilderAndJoin() throws InterruptedException {
        int[] result = {0};
        Thread built = Thread.ofPlatform().name("builder-worker")
                .daemon(false).priority(Thread.NORM_PRIORITY).unstarted(() -> result[0] = 42);
        System.out.println("Builder unstarted: " + built.getState() + ", daemon=" + built.isDaemon()
                + ", priority=" + built.getPriority());
        built.start();
        // NOTICE: успешный join обеспечивает видимость действий завершившегося потока.
        // Поэтому result можно прочитать без volatile после ожидания завершения built.
        built.join();
        System.out.println("После join результат обычного поля: " + result[0]);
        if (result[0] != 42) {
            throw new AssertionError("join должен опубликовать результат");
        }
    }
}
