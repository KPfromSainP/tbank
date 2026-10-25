package academy.backend.market_pulse.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.exception.InvalidInstrumentException;
import academy.backend.market_pulse.model.Bond;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Etf;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Stock;

class InstrumentFactoriesTest {

    @ParameterizedTest(name = "тип {0}")
    @EnumSource(InstrumentType.class)
    void createsInstrumentOfRequestedType(InstrumentType type) {
        Instrument instrument = InstrumentFactories.create(type, "TEST", "Тест", Currency.RUB);

        assertThat(instrument.getType()).isEqualTo(type);
        assertThat(instrument.getTicker()).isEqualTo("TEST");
        assertThat(instrument.getCurrency()).isEqualTo(Currency.RUB);
    }

    @Test
    void knownTypesMapToTheExpectedClasses() {
        assertThat(InstrumentFactories.create(InstrumentType.STOCK, "A", "a", Currency.RUB)).isInstanceOf(Stock.class);
        assertThat(InstrumentFactories.create(InstrumentType.BOND, "B", "b", Currency.RUB)).isInstanceOf(Bond.class);
        assertThat(InstrumentFactories.create(InstrumentType.ETF, "C", "c", Currency.USD)).isInstanceOf(Etf.class);
    }

    @Test
    void invalidInstrumentIsRejectedByTheFactory() {
        assertThatThrownBy(() -> InstrumentFactories.create(InstrumentType.STOCK, " ", "Тест", Currency.RUB))
                .isInstanceOf(InvalidInstrumentException.class)
                .hasMessageContaining("Тикер");
    }
}
