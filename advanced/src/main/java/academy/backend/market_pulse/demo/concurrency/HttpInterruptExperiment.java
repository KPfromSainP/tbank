package academy.backend.market_pulse.demo.concurrency;

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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Один опыт отмены запроса: сервер отправляет заголовки, а тело удерживает до разрешения main.
 * Владеет сервером, клиентом и работником и освобождает их при завершении опыта.
 */
final class HttpInterruptExperiment implements AutoCloseable {
    /**
     * true выбирает HttpClient.send, false — чтение через HttpURLConnection.
     */
    private final boolean modern;
    /**
     * Подтверждает, что сервер уже получил запрос и отправил заголовки.
     */
    private final CountDownLatch headersSent = new CountDownLatch(1);
    /**
     * Удерживает тело ответа, пока основной поток проверяет реакцию на interrupt.
     */
    private final CountDownLatch releaseBody = new CountDownLatch(1);
    /**
     * Сигнал завершения worker-потока, независимый от успешности запроса.
     */
    private final CountDownLatch finished = new CountDownLatch(1);
    /**
     * Передаёт ошибку worker-потока основному потоку для проверки результата опыта.
     */
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    /**
     * Заранее известное тело ответа, позволяющее проверить завершившееся чтение.
     */
    private final byte[] body = "quote=42".getBytes(StandardCharsets.UTF_8);
    /**
     * Локальный сервер на свободном порту; внешняя сеть для опыта не используется.
     */
    private final HttpServer server;
    /**
     * Клиент создаётся до worker-потока, чтобы interrupt не пришёлся на его инициализацию.
     */
    private final HttpClient httpClient;
    /**
     * Worker выполняет единственный запрос и получает interrupt из main.
     */
    private final Thread worker;

    HttpInterruptExperiment(boolean modern) throws IOException {
        this.modern = modern;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/quote", this::respond);
        this.httpClient = modern ? HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build() : null;
        this.worker = Thread.ofPlatform()
                .name("http-interrupt-demo")
                .unstarted(this::request);
    }

    /**
     * Прерывает запрос после заголовков и проверяет, нужен ли ответ для завершения worker-потока.
     */
    void run() throws InterruptedException {
        System.out.println("\n--- " + (modern ? "HttpClient.send" : "HttpURLConnection") + ": платформенный поток ---");
        server.start();
        worker.start();
        if (!headersSent.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Сервер не получил запрос вовремя");
        }
        System.out.println("Заголовки отправлены; тело удерживается сервером. Вызываем interrupt().");
        worker.interrupt();
        verifyCancellation();
        if (failure.get() != null) {
            throw new AssertionError("Ошибка демонстрации отмены запроса", failure.get());
        }
    }

    /**
     * Отправляет заголовки сразу, а тело — только после разрешения основного потока.
     */
    private void respond(HttpExchange exchange) {
        try (exchange) {
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().flush();
            headersSent.countDown();
            if (releaseBody.await(10, TimeUnit.SECONDS)) {
                exchange.getResponseBody().write(body);
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (IOException error) {
            System.out.println("Сервер: клиент закрыл соединение (" + error.getClass().getSimpleName() + ")");
        }
    }

    private void request() {
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/quote");
        try {
            if (modern) {
                receiveWithHttpClient(uri);
            } else {
                receiveWithUrlConnection(uri);
            }
        } catch (Throwable error) {
            failure.set(error);
        } finally {
            finished.countDown();
        }
    }

    /**
     * Проверяет прерываемое ожидание send и восстанавливает сброшенный исключением флаг.
     */
    private void receiveWithHttpClient(URI uri) throws IOException {
        try {
            httpClient.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
            failure.set(new AssertionError("send неожиданно получил удерживаемое тело"));
        } catch (InterruptedException error) {
            System.out.println("send: InterruptedException, флаг в catch=" + Thread.currentThread().isInterrupted());
            Thread.currentThread().interrupt();
            System.out.println("send: флаг восстановлен, worker выходит");
        }
    }

    /**
     * Читает тело через API, у которого interrupt платформенного потока не отменяет чтение.
     */
    private void receiveWithUrlConnection(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
        connection.setConnectTimeout(3_000);
        connection.setReadTimeout(5_000);
        try (var input = connection.getInputStream()) {
            String response = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("URLConnection: тело=" + response + ", флаг=" + Thread.currentThread().isInterrupted());
            if (!response.equals("quote=42") || !Thread.currentThread().isInterrupted()) {
                failure.set(new AssertionError("Ожидались ответ и сохранённый флаг прерывания"));
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Проверяет разницу между отменой send и завершением чтения только после получения тела.
     */
    private void verifyCancellation() throws InterruptedException {
        if (modern) {
            if (!finished.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("HttpClient не вышел по прерыванию");
            }
            // NOTICE: отмена ожидания клиента не откатывает уже начатую работу сервера.
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
    }

    @Override
    public void close() throws InterruptedException {
        releaseBody.countDown();
        server.stop(0);
        worker.interrupt();
        try {
            worker.join(7_000);
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }
}
