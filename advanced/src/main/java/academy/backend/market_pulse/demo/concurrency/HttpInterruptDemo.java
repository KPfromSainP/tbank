package academy.backend.market_pulse.demo.concurrency;

/**
 * Сравнивает отмену HttpClient.send и HttpURLConnection на локальном сервере с задержкой тела.
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
        try (var experiment = new HttpInterruptExperiment(modern)) {
            experiment.run();
        }
    }
}
