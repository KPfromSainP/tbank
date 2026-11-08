package academy.backend.market_pulse.refactoring.nesting;

import java.util.Optional;

class OrderValidatorV1Test extends OrderValidatorContractTest {

    private final OrderValidatorV1 validator = new OrderValidatorV1();

    @Override
    Optional<String> firstError(OrderDraft draft) {
        return validator.firstError(draft);
    }
}
