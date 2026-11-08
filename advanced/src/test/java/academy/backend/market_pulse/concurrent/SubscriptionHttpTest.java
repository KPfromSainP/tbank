package academy.backend.market_pulse.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import academy.backend.market_pulse.client.impl.HttpClientTInvestApiClient;
import academy.backend.market_pulse.config.TInvestApiProperties;
import academy.backend.market_pulse.dto.SubscriptionRequest;
import academy.backend.market_pulse.dto.SubscriptionSessionOptions;
import academy.backend.market_pulse.dto.SubscriptionTicker;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Timeout;

/** Проверяет отмену настоящего HTTP-клиента сервиса до получения тела ответа. */
@Timeout(10)
class SubscriptionHttpTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cancellationInterruptsHttpBeforeResponseBodyArrives(boolean remove) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch request = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var handlers = Executors.newCachedThreadPool()) {
            server.setExecutor(handlers);
            server.createContext("/v1/instruments", exchange -> {
                try (exchange) {
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
            var output = new java.util.concurrent.CopyOnWriteArrayList<String>();
            QuoteSubscriptionService service = new QuoteSubscriptionService(
                    new SubscriptionSessionOptions(1), client, output::add);
            try {
                service.watch(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofMinutes(1)));
                assertTrue(request.await(2, TimeUnit.SECONDS));
                assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                    if (remove) {
                        assertTrue(service.remove(new SubscriptionTicker("SBER")));
                    } else {
                        service.close();
                    }
                });
            } finally {
                release.countDown();
                server.stop(0);
                service.close();
            }
            assertTrue(output.isEmpty(), "Отмена не должна печатать сообщение о сетевой ошибке");
        } finally {
            server.stop(0);
        }
    }
}
