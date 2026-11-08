package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.exception.TInvestApiException;
import academy.backend.market_pulse.factory.InstrumentFactories;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Quote;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;

/** Клиент для тестов: управляемое ожидание запроса и каталог из двух инструментов. */
final class SubscriptionTestClient {
    /** Создаёт клиент, который сохраняет прерывание так же, как HttpClientTInvestApiClient. */
    static TInvestApiClient from(Callable<BigDecimal> request) {
        return () -> {
            try {
                BigDecimal price = request.call();
                return List.of(quote("SBER", price), quote("GAZP", price));
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new TInvestApiException("Запрос прерван", error);
            } catch (RuntimeException error) {
                throw error;
            } catch (Exception error) {
                throw new TInvestApiException("Ошибка запроса", error);
            }
        };
    }

    /** Создаёт полноценную котировку с инструментом для выбора по тикеру в реальном сервисе. */
    private static Quote quote(String ticker, BigDecimal price) {
        return new Quote(InstrumentFactories.create(InstrumentType.STOCK, ticker, ticker, Currency.RUB),
                price, BigDecimal.ZERO);
    }
}
