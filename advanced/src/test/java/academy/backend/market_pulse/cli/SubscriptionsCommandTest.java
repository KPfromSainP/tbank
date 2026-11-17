package academy.backend.market_pulse.cli;

import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.client.InterruptibleTInvestApiClient;
import academy.backend.market_pulse.client.impl.HttpUrlConnectionTInvestApiClient;
import academy.backend.market_pulse.client.impl.reactor.ReactorTInvestApiClient;
import academy.backend.market_pulse.dto.subscription.SubscriptionResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import academy.backend.market_pulse.dto.subscription.SubscriptionEngine;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionsCommandTest {
    @Test
    void formatsPriceWithoutLosingScaleOrUsingScientificNotation() {
        assertEquals("[SBER] 250.00", SubscriptionsCommand.formatResult(
                new SubscriptionResult.PriceUpdate("SBER", new BigDecimal("250.00"))));
        assertEquals("[GAZP] 1000", SubscriptionsCommand.formatResult(
                new SubscriptionResult.PriceUpdate("GAZP", new BigDecimal("1E+3"))));
    }

    @Test
    void formatsFailureAtTheConsoleBoundary() {
        assertEquals("[SBER] Ошибка: network failed", SubscriptionsCommand.formatResult(
                new SubscriptionResult.Failure("SBER", new IllegalStateException("network failed"))));
    }

    @ParameterizedTest
    @EnumSource(SubscriptionEngine.class)
    void acceptsAnAlternativeClientWithTheInterruptibleContract(SubscriptionEngine engine) {
        InterruptibleTInvestApiClient client = List::of;
        System.setIn(new ByteArrayInputStream(new byte[0]));

        assertEquals(0, new CommandLine(new SubscriptionsCommand(client)).execute("--engine", engine.name()));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Подписки и пул остановлены"));
    }

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();
    private InputStream originalIn;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void captureConsole() {
        originalIn = System.in;
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errors, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreConsole() {
        System.setIn(originalIn);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @ParameterizedTest
    @ValueSource(classes = {ReactorTInvestApiClient.class, HttpUrlConnectionTInvestApiClient.class})
    void rejectsNonInterruptibleClientBeforeReadingCommands(Class<? extends TInvestApiClient> type) {
        var input = new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8));
        System.setIn(input);

        int exitCode = new CommandLine(new SubscriptionsCommand(mock(type))).execute();

        assertEquals(2, exitCode);
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("HTTP_CLIENT"));
        assertEquals(5, input.available(), "Неподдерживаемый режим не должен начинать сессию");
    }

    @Test
    void eofClosesSupportedSession() {
        System.setIn(new ByteArrayInputStream(new byte[0]));

        int exitCode = new CommandLine(new SubscriptionsCommand(mock(InterruptibleTInvestApiClient.class))).execute();

        assertEquals(0, exitCode);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Подписки и пул остановлены"));
    }

    @Test
    void invalidCommandsDoNotEndSessionAndSystemInputStaysOpen() {
        AtomicBoolean inputClosed = new AtomicBoolean();
        System.setIn(new ByteArrayInputStream("""
                watch SBER 0s 1m
                watch BAD! 1s 1m
                watch sber 1s 1m
                watch SBER 1s 1m
                remove sber
                remove SBER
                exit
                """.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() { inputClosed.set(true); }
        });
        var client = mock(InterruptibleTInvestApiClient.class);
        when(client.fetchQuotes()).thenReturn(List.of());

        assertEquals(0, new CommandLine(new SubscriptionsCommand(client)).execute("--workers", "1"));

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Ошибка: Длительность"));
        assertTrue(printed.contains("Ошибка: Некорректный тикер"));
        assertTrue(printed.contains("Подписка принята: SBER"));
        assertTrue(printed.contains("Подписка уже существует: SBER"));
        assertTrue(printed.contains("Подписка удалена: SBER"));
        assertTrue(printed.contains("Подписка не найдена: SBER"));
        assertFalse(inputClosed.get());
    }

    @Test
    void invalidWorkerCountIsRejectedBeforeReadingCommands() {
        var input = new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8));
        System.setIn(input);

        assertEquals(2, new CommandLine(new SubscriptionsCommand(mock(InterruptibleTInvestApiClient.class)))
                .execute("--workers", "0"));

        assertEquals(5, input.available());
    }

    @Test
    void readFailureClosesSessionThreads() {
        var existingThreads = Thread.getAllStackTraces().keySet();
        System.setIn(new InputStream() {
            @Override
            public int read() throws IOException { throw new IOException("input failed"); }
        });

        assertEquals(1, new CommandLine(new SubscriptionsCommand(mock(InterruptibleTInvestApiClient.class))).execute());

        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("input failed"));
        assertTrue(Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> !existingThreads.contains(thread))
                .noneMatch(thread -> thread.getName().equals("quote-dispatcher")
                        || thread.getName().startsWith("quote-worker-")));
    }

    @Test
    void interruptedShutdownReturns130AndPreservesInterrupt() {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        var command = new CommandLine(new SubscriptionsCommand(mock(InterruptibleTInvestApiClient.class)));
        Thread.currentThread().interrupt();
        try {
            assertEquals(130, command.execute());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
