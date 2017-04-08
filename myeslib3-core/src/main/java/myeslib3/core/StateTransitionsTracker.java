package myeslib3.core;

import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Event;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class StateTransitionsTracker<A extends AggregateRoot> {

	final A originalInstance;
	final StateTransitionFn<A> applyEventsFn;
	final DependencyInjectionFn<A> dependencyInjectionFn;
	final List<StateTransition<A>> stateTransitions;

	public StateTransitionsTracker(A originalInstance,
																 StateTransitionFn<A> applyEventsFn,
																 DependencyInjectionFn<A> dependencyInjectionFn) {
		requireNonNull(originalInstance);
		requireNonNull(applyEventsFn);
		requireNonNull(dependencyInjectionFn);
		this.originalInstance = originalInstance;
		this.applyEventsFn = applyEventsFn;
		this.dependencyInjectionFn = dependencyInjectionFn;
		this.stateTransitions = new ArrayList<>();
	}

	public StateTransitionsTracker<A> applyEvents(List<Event> events) {
		requireNonNull(events);
		events.forEach(e -> {
			final A newInstance = applyEventsFn.apply(e, currentState());
			stateTransitions.add(new StateTransition<>(newInstance, e));
		});
		return this;
	}

	public List<Event> collectedEvents() {
		return stateTransitions.stream().map(t -> t.afterThisEvent).collect(Collectors.toList());
	}

	public A currentState() {
		final A current = stateTransitions.size() == 0 ?
						originalInstance : stateTransitions.get(stateTransitions.size() - 1).newInstance;
		return dependencyInjectionFn.inject(current);
	}

  public boolean isEmpty() {
    return stateTransitions.isEmpty();
  }

  class StateTransition<T extends AggregateRoot> {
		private final T newInstance;
		private final Event afterThisEvent;

		StateTransition(T newInstance, Event afterThisEvent) {
			requireNonNull(newInstance);
      requireNonNull(afterThisEvent);
			this.newInstance = newInstance;
			this.afterThisEvent = afterThisEvent;
		}
	}
}
