package myeslib3.core.query;

import myeslib3.core.AggregateRoot;
import myeslib3.core.Command;
import myeslib3.core.Event;

import java.util.Optional;

@FunctionalInterface
public interface SagaEventMonitoringFn<ENTITY_PROJECTION extends AggregateRoot, COMMAND extends Command> {

	Optional<COMMAND> apply(Event event, ENTITY_PROJECTION instance);

}
