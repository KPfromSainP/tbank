package academy.backend.market_pulse.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.exception.TInvestApiException;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Etf;
import academy.backend.market_pulse.model.Quote;

/** Команда {@code update} на моке клиента: сеть не нужна, результат и сообщения проверяются по выводу. */
@ExtendWith(MockitoExtension.class)
class UpdateCommandTest {

    @Mock
    private TInvestApiClient client;

    @InjectMocks
    private UpdateCommand command;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void captureOutput() {
        originalOut = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
    }

    private static Quote quote(String ticker, String price) {
        return new Quote(new Etf(ticker, ticker, Currency.RUB, "-"), new BigDecimal(price), BigDecimal.ONE);
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void printsEveryQuoteAndTheirCount() {
        when(client.fetchQuotes()).thenReturn(List.of(quote("SBER", "250"), quote("FXUS", "6.85")));

        assertThat(command.call()).isZero();

        assertThat(printed()).contains("SBER: 250").contains("FXUS: 6.85").contains("Получено котировок: 2");
        verify(client).fetchQuotes();
    }

    @Test
    void emptyResultIsStillASuccess() {
        when(client.fetchQuotes()).thenReturn(List.of());

        assertThat(command.call()).isZero();

        assertThat(printed()).contains("Получено котировок: 0");
    }

    @Test
    void failureOfTheClientIsReportedAndTheExitCodeIsOne() {
        when(client.fetchQuotes()).thenThrow(new TInvestApiException("сервис недоступен"));

        assertThat(command.call()).isEqualTo(1);

        assertThat(printed()).contains("Не удалось обновить котировки: сервис недоступен");
    }
}
