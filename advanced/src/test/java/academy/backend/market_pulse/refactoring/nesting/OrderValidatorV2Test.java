package academy.backend.market_pulse.refactoring.nesting;

import java.util.Optional;

class OrderValidatorV2Test extends OrderValidatorContractTest {

    private final OrderValidatorV2 validator = new OrderValidatorV2();

    @Override
    Optional<String> firstError(OrderDraft draft) {
        return validator.firstError(draft);
    }
}
