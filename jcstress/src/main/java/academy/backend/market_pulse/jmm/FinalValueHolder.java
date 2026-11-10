package academy.backend.market_pulse.jmm;

/**
 * Объект с final-полем; конструктор не передаёт this другим потокам до завершения инициализации.
 */
final class FinalValueHolder {
    /**
     * Значение, которое читатель корректно сконструированного объекта должен увидеть вместо нуля.
     */
    final int value;

    FinalValueHolder() {
        this.value = 42;
    }
}
