package academy.backend.market_pulse.demo.concurrency;

/** Публикация результата: обычные данные становятся видимыми после чтения volatile-флага. */
public final class PublicationDemo {
    /** Запускает читателя и публикует значение из основного потока. */
    public static void main(String[] args) throws InterruptedException {
        ResultProvider provider = new ResultProvider();
        Thread reader = Thread.ofPlatform().name("result-reader").unstarted(() -> printResult(provider));
        reader.start();
        try {
            provider.complete(42);
            reader.join();
        } finally {
            reader.interrupt();
            reader.join();
        }
    }

    /** Читает опубликованное значение либо завершает ожидание с сохранённым флагом прерывания. */
    private static void printResult(ResultProvider provider) {
        try {
            // NOTICE: активное ожидание расходует процессор. onSpinWait лишь даёт подсказку
            // процессору, а видимость данных обеспечивает volatile внутри ResultProvider.
            System.out.println("Опубликовано: " + provider.await());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
