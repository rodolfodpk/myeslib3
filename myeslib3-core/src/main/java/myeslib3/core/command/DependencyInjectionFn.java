package myeslib3.core.command;

import myeslib3.core.AggregateRoot;

@FunctionalInterface
public interface DependencyInjectionFn<AGGREGATE_ROOT extends AggregateRoot> {

	AGGREGATE_ROOT inject(AGGREGATE_ROOT aggregateRoot);

}
