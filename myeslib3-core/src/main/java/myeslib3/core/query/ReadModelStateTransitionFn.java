package myeslib3.core.query;

import myeslib3.core.Event;

@FunctionalInterface
public interface ReadModelStateTransitionFn<ENTITY_PROJECTION> {

	ENTITY_PROJECTION apply(Event event, ENTITY_PROJECTION instance);

}
