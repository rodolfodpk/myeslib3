package myeslib3.core.functions;

@FunctionalInterface
public interface StateTransitionFn<AggregateRoot, Event> {
    AggregateRoot apply(Event event, AggregateRoot aggregateRoot);
}
