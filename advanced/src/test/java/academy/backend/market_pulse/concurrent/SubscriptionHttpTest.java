package academy.backend.market_pulse.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import academy.backend.market_pulse.client.impl.HttpClientTInvestApiClient;
import academy.backend.market_pulse.config.TInvestApiProperties;
import academy.backend.market_pulse.dto.subscription.SubscriptionConfig;
import academy.backend.market_pulse.dto.subscription.SubscriptionRequest;
import academy.backend.market_pulse.dto.subscription.SubscriptionResult;

import java.math.BigDecimal;
import academy.backend.market_pulse.dto.subscription.SubscriptionTicker;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Проверяет отмену настоящего HTTP-клиента сервиса до получения тела ответа.
 */
@Timeout(10)
class SubscriptionHttpTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cancellationInterruptsHttpBeforeResponseBodyArrives(boolean remove) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch request = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch nextResult = new CountDownLatch(1);
        AtomicInteger requests = new AtomicInteger();
        try (var handlers = Executors.newCachedThreadPool()) {
            server.setExecutor(handlers);
            server.createContext("/v1/instruments", exchange -> {
                try (exchange) {
                    if (requests.incrementAndGet() > 1) {
                        byte[] body = """
                                [{"ticker":"GAZP","name":"Газпром","type":"STOCK","currency":"RUB",
                                  "price":120.50,"changePercent":0}]
                                """.getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(200, body.length);
                        exchange.getResponseBody().write(body);
                        return;
                    }
                    exchange.sendResponseHeaders(200, 2);
                    exchange.getResponseBody().flush();
                    request.countDown();
                    release.await(5, TimeUnit.SECONDS);
                    exchange.getResponseBody().write("42".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.io.IOException ignored) {
                    // Отменивший запрос клиент мог закрыть соединение.
                }
            });
            server.start();
            var client = new HttpClientTInvestApiClient(new TInvestApiProperties("127.0.0.1",
                    server.getAddress().getPort(), Duration.ofSeconds(2), Duration.ofSeconds(5)));
            var output = new java.util.concurrent.CopyOnWriteArrayList<SubscriptionResult>();
            QuoteSubscriptionService service = new QuoteSubscriptionService(
                    new SubscriptionConfig(1), client, line -> {
                        output.add(line);
                        nextResult.countDown();
                    }, error -> fail("Неожиданная ошибка задачи", error));
            try {
                service.watch(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofMinutes(1)));
                assertTrue(request.await(2, TimeUnit.SECONDS));
                assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                    if (remove) {
                        assertTrue(service.remove(new SubscriptionTicker("SBER")));
                        service.watch(new SubscriptionRequest("GAZP", Duration.ofMinutes(1), Duration.ofMinutes(1)));
                        assertTrue(nextResult.await(1, TimeUnit.SECONDS),
                                "После remove единственный worker должен освободиться до ответа первого сервера");
                    } else {
                        service.close();
                    }
                });
            } finally {
                release.countDown();
                server.stop(0);
                service.close();
            }
            assertEquals(remove ? List.of(new SubscriptionResult.PriceUpdate("GAZP", new BigDecimal("120.50"))) : List.of(), output,
                    "Отмена не должна печатать результат или сетевую ошибку SBER");
        } finally {
            server.stop(0);
        }
    }
}
