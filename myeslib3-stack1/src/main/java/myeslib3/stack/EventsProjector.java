package myeslib3.stack;

import myeslib3.core.data.UnitOfWork;

@FunctionalInterface
public interface EventsProjector {

	void submit(final UnitOfWork unitOfWork);

}

