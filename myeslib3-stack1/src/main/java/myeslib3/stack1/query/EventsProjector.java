package myeslib3.stack1.query;

import myeslib3.core.data.UnitOfWork;

import java.util.List;

@FunctionalInterface
public interface EventsProjector {

	void apply(UnitOfWork unitOfWork);

}

