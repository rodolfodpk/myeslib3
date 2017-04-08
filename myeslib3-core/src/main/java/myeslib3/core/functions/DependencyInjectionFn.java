package myeslib3.core.functions;

import myeslib3.core.data.AggregateRoot;

@FunctionalInterface
public interface DependencyInjectionFn<A extends AggregateRoot> {

	A inject(A aggregateRoot);

}
