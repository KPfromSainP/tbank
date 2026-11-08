package academy.backend.market_pulse.demo.concurrency;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Два блокирующих HTTP API на платформенных потоках: send прерывается, URLConnection продолжает чтение.
 * Без аргументов запускаются оба опыта; можно передать http-client или url-connection.
 * Сервер отправляет заголовки, а тело удерживает до команды main. Внешняя сеть не нужна.
 */
public final class HttpInterruptDemo {
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "both" : args[0];
        switch (mode) {
            case "both" -> { demonstrate(true); demonstrate(false); }
            case "http-client" -> demonstrate(true);
            case "url-connection" -> demonstrate(false);
            default -> throw new IllegalArgumentException("Режим: both | http-client | url-connection");
        }
    }

    private static void demonstrate(boolean modern) throws Exception {
        String name = modern ? "HttpClient.send" : "HttpURLConnection";
        System.out.println("\n--- " + name + ": платформенный поток ---");
        CountDownLatch headersSent = new CountDownLatch(1);
        CountDownLatch releaseBody = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        byte[] body = "quote=42".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/quote", exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().flush();
                headersSent.countDown();
                if (!releaseBody.await(10, TimeUnit.SECONDS)) {
                    return;
                }
                exchange.getResponseBody().write(body);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                // После отмены клиент вправе закрыть соединение до отправки тела.
                System.out.println("Сервер: клиент закрыл соединение (" + e.getClass().getSimpleName() + ")");
            }
        });
        server.start();
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/quote");
        // Создание клиента вне worker: прерываем ожидание запроса, а не инициализацию клиента.
        HttpClient httpClient = modern ? HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build() : null;
        Thread worker = Thread.ofPlatform().name("http-interrupt-demo").unstarted(() -> {
            try {
                if (modern) {
                    try {
                        httpClient.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
                        failure.set(new AssertionError("send неожиданно получил удерживаемое тело"));
                    } catch (InterruptedException e) {
                        System.out.println("send: InterruptedException, флаг в catch="
                                + Thread.currentThread().isInterrupted());
                        Thread.currentThread().interrupt();
                        System.out.println("send: флаг восстановлен, worker выходит");
                    }
                } else {
                    HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
                    connection.setConnectTimeout(3_000);
                    connection.setReadTimeout(5_000); // Страховка, не механизм демонстрируемой отмены.
                    try (var input = connection.getInputStream()) {
                        String response = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                        System.out.println("URLConnection: тело=" + response + ", флаг="
                                + Thread.currentThread().isInterrupted());
                        if (!response.equals("quote=42") || !Thread.currentThread().isInterrupted()) {
                            failure.set(new AssertionError("Ожидались ответ и сохранённый флаг прерывания"));
                        }
                    } finally {
                        connection.disconnect();
                    }
                }
            } catch (Throwable e) {
                failure.set(e);
            } finally {
                finished.countDown();
            }
        });
        try {
            worker.start();
            if (!headersSent.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Сервер не получил запрос вовремя");
            }
            System.out.println("Заголовки отправлены; тело удерживается сервером. Вызываем interrupt().");
            worker.interrupt();
            if (modern) {
                if (!finished.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("HttpClient не вышел по прерыванию");
                }
                System.out.println("worker завершился до отправки тела; отмена не откатывает работу сервера");
            } else {
                if (finished.await(400, TimeUnit.MILLISECONDS)) {
                    throw new AssertionError("URLConnection завершился до отправки тела", failure.get());
                }
                System.out.println("После interrupt worker всё ещё ожидает тело. Теперь сервер отправит его.");
                releaseBody.countDown();
                if (!finished.await(7, TimeUnit.SECONDS)) {
                    throw new AssertionError("URLConnection не завершил чтение");
                }
            }
            if (failure.get() != null) {
                throw new AssertionError("Ошибка демонстрации " + name, failure.get());
            }
        } finally {
            releaseBody.countDown();
            server.stop(0);
            worker.interrupt();
            worker.join(7_000);
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }
}
