package academy.backend.market_pulse.demo.concurrency;

/** Однократная публикация обычного поля результата через volatile-флаг готовности. */
final class ResultProvider {
    /** Обычное поле; его видимость обеспечивается флагом готовности, а не самим полем. */
    private int result;
    /** Однократный сигнал публикации предшествующей записи result. */
    private volatile boolean resultsReady;

    /** Записывает данные до установки флага, чтобы читатель флага увидел и результат. */
    void complete(int value) {
        result = value;
        // NOTICE: volatile-запись публикует все предшествующие записи этого потока.
        // Читатель, увидевший true, увидит и result; обычный флаг этого не гарантирует.
        resultsReady = true;
    }

    /** Ожидает публикации в активном цикле, завершая ожидание по запросу прерывания. */
    int await() throws InterruptedException {
        while (!resultsReady) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Ожидание результата отменено");
            }
            Thread.onSpinWait();
        }
        return result;
    }
}
