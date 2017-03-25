package myeslib3.stack1.query;

import myeslib3.core.UnitOfWork;

import java.util.List;

@FunctionalInterface
public interface EventsProjector {

	void apply(String aggregateRootId, final List<UnitOfWork> unitOfWork);

}

