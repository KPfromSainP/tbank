package academy.backend.market_pulse.refactoring.errors;

import java.util.List;

class QuoteImporterV2Test extends QuoteImporterContractTest {

    private final QuoteImporterV2 importer = new QuoteImporterV2();

    @Override
    ImportResult importLines(List<String> lines) {
        return importer.importLines(lines);
    }
}
