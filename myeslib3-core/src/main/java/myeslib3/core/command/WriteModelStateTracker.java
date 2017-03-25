package myeslib3.core.command;

import myeslib3.core.AggregateRoot;
import myeslib3.core.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WriteModelStateTracker<AGGREGATE_ROOT extends AggregateRoot> {

	final AGGREGATE_ROOT originalInstance;
	final WriteModelStateTransitionFn<AGGREGATE_ROOT> applyEventsFn;
	final DependencyInjectionFn<AGGREGATE_ROOT> dependencyInjectionFn;
	final List<StateTransition<AGGREGATE_ROOT>> stateTransitions;

	public WriteModelStateTracker(AGGREGATE_ROOT originalInstance,
																WriteModelStateTransitionFn<AGGREGATE_ROOT> applyEventsFn,
																DependencyInjectionFn<AGGREGATE_ROOT> dependencyInjectionFn) {
		this.originalInstance = originalInstance;
		this.applyEventsFn = applyEventsFn;
		this.dependencyInjectionFn = dependencyInjectionFn;
		this.stateTransitions = new ArrayList<>();
	}

	public WriteModelStateTracker<AGGREGATE_ROOT> applyEvents(List<Event> events) {
		events.forEach(e -> {
			final AGGREGATE_ROOT newInstance = applyEventsFn.apply(e, currentState());
			stateTransitions.add(new StateTransition<>(newInstance, e));
		});
		return this;
	}

	public List<Event> collectedEvents() {
		return stateTransitions.stream().map(t -> t.afterThisEvent).collect(Collectors.toList());
	}

	public AGGREGATE_ROOT currentState() {
		final AGGREGATE_ROOT current = stateTransitions.size() == 0 ?
						originalInstance : stateTransitions.get(stateTransitions.size() - 1).newInstance;
		return dependencyInjectionFn.inject(current);
	}

	class StateTransition<AGGREGATE_ROOT> {
		private final AGGREGATE_ROOT newInstance;
		private final Event afterThisEvent;

		StateTransition(AGGREGATE_ROOT newInstance, Event afterThisEvent) {
			this.newInstance = newInstance;
			this.afterThisEvent = afterThisEvent;
		}
	}
}
