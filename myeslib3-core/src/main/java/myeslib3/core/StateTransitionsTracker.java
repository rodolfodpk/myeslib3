package myeslib3.core;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@AllArgsConstructor
public class StateTransitionsTracker<A extends AggregateRoot> {

	@NonNull final A originalInstance;
	@NonNull final BiFunction<Event, A, A> applyEventsFn;
	@NonNull final Function<A, A> dependencyInjectionFn;
	final List<StateTransition<A>> stateTransitions = new ArrayList<>();

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
		return dependencyInjectionFn.apply(current);
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
