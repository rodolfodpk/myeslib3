package myeslib3.stack1.query;

import myeslib3.core.data.UnitOfWork;

@FunctionalInterface
public interface EventsProjector {

	void apply(UnitOfWork unitOfWork);

}

