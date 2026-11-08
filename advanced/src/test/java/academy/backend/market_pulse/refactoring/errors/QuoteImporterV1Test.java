package academy.backend.market_pulse.refactoring.errors;

import java.util.List;

class QuoteImporterV1Test extends QuoteImporterContractTest {

    private final QuoteImporterV1 importer = new QuoteImporterV1();

    @Override
    ImportResult importLines(List<String> lines) {
        return importer.importLines(lines);
    }
}
