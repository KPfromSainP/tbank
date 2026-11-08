package academy.backend.market_pulse.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class SubscriptionHttpTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cancellationInterruptsHttpBeforeResponseBodyArrives(boolean remove) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch request = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var handlers = Executors.newCachedThreadPool(); var client = HttpClient.newHttpClient()) {
            server.setExecutor(handlers);
            server.createContext("/quote", exchange -> {
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
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/quote");
            QuoteSubscriptionService service = new QuoteSubscriptionService(1, ticker -> {
                String body = client.send(HttpRequest.newBuilder(uri).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
                return new BigDecimal(body);
            }, line -> {});
            try {
                service.watch("SBER", Duration.ofSeconds(1), Duration.ofMinutes(1));
                assertTrue(request.await(2, TimeUnit.SECONDS));
                assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                    if (remove) {
                        assertTrue(service.remove("SBER"));
                    } else {
                        service.close();
                    }
                });
            } finally {
                release.countDown();
                server.stop(0);
                service.close();
            }
        } finally {
            server.stop(0);
        }
    }
}
