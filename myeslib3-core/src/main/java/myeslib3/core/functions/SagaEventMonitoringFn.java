package myeslib3.core.functions;

import myeslib3.core.data.Command;
import myeslib3.core.data.Event;

import java.util.Optional;

@FunctionalInterface
public interface SagaEventMonitoringFn {

	Optional<Command> apply(Event event);

}
