package academy.backend.market_pulse.demo.concurrency;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

/** Legacy Socket.read: закрытие ресурса отменяет ожидание. Проверять ОС/стек через jcmd. */
public final class BlockingIoDemo {
    public static void main(String[] args) throws Exception {
        try (var server = new ServerSocket(0); var client = new Socket("127.0.0.1", server.getLocalPort());
                var peer = server.accept()) {
            var aboutToRead = new CountDownLatch(1);
            Thread reader = Thread.ofPlatform().name("legacy-socket-read").start(() -> {
                try { aboutToRead.countDown(); System.out.println("read=" + client.getInputStream().read()); }
                catch (java.io.IOException e) { System.out.println("I/O завершён: " + e.getClass().getSimpleName()); }
            });
            try {
                aboutToRead.await();
                System.out.println("PID=" + ProcessHandle.current().pid() + ", state=" + reader.getState());
                reader.interrupt();
                System.out.println("Отправлен interrupt. Enter — закрыть сокет и выйти.");
                System.in.read();
            } finally { client.close(); reader.join(); }
        }
    }
}
