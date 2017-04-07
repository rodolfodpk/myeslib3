package myeslib3.core.functions;

import myeslib3.core.data.AggregateRoot;

@FunctionalInterface
public interface DependencyInjectionFn<AGGREGATE_ROOT extends AggregateRoot> {

	AGGREGATE_ROOT inject(AGGREGATE_ROOT aggregateRoot);

}
