package myeslib3.core.functions;

import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Event;

@FunctionalInterface
public interface StateTransitionFn<A extends AggregateRoot> {

    A apply(Event event, A aggregateRoot);

}
