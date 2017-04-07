package myeslib3.core.functions;

import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Command;
import myeslib3.core.data.Event;

import java.util.Optional;

@FunctionalInterface
public interface SagaEventMonitoringFn<ENTITY_PROJECTION extends AggregateRoot, COMMAND extends Command> {

	Optional<COMMAND> apply(Event event, ENTITY_PROJECTION instance);

}
