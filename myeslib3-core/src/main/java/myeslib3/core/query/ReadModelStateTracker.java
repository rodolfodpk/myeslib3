package myeslib3.core.query;

import myeslib3.core.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReadModelStateTracker<ENTITY_PROJECTION> {

	final ENTITY_PROJECTION originalInstance;
	final ReadModelStateTransitionFn<ENTITY_PROJECTION> applyEventsFn;
	final List<ReadModelStateTransition<ENTITY_PROJECTION>> stateTransitions;

	public ReadModelStateTracker(ENTITY_PROJECTION originalInstance,
															 ReadModelStateTransitionFn<ENTITY_PROJECTION> applyEventsFn) {
		this.originalInstance = originalInstance;
		this.applyEventsFn = applyEventsFn;
		this.stateTransitions = new ArrayList<>();
	}

	public ReadModelStateTracker<ENTITY_PROJECTION> applyEvents(List<Event> events) {
		events.forEach(e -> {
			final ENTITY_PROJECTION newInstance = applyEventsFn.apply(e, currentState());
			stateTransitions.add(new ReadModelStateTransition<>(newInstance, e));
		});
		return this;
	}

	public List<Event> collectedEvents() {
		return stateTransitions.stream().map(t -> t.afterThisEvent).collect(Collectors.toList());
	}

	public ENTITY_PROJECTION currentState() {
		final ENTITY_PROJECTION current = stateTransitions.size() == 0 ?
						originalInstance : stateTransitions.get(stateTransitions.size() - 1).newInstance;
		return current;
	}

	class ReadModelStateTransition<ENTITY_PROJECTION> {
		private final ENTITY_PROJECTION newInstance;
		private final Event afterThisEvent;

		ReadModelStateTransition(ENTITY_PROJECTION newInstance, Event afterThisEvent) {
			this.newInstance = newInstance;
			this.afterThisEvent = afterThisEvent;
		}
	}
}
