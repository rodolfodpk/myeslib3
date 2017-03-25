package myeslib3.core.command;

import myeslib3.core.AggregateRoot;
import myeslib3.core.Event;

@FunctionalInterface
public interface WriteModelStateTransitionFn<A extends AggregateRoot> {

	A apply(Event event, A instance);

}
