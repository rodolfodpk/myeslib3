package myeslib3.stack.projection;

import myeslib3.core.data.UnitOfWork;

public interface EventsProjector {

  void submit(final UnitOfWork unitOfWork);

}

