package myeslib3.core.functions;

@FunctionalInterface
public interface DependencyInjectionFn<AggregateRoot> {
    AggregateRoot inject(AggregateRoot aggregateRoot);
}
