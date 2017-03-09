package myeslib3.stack1.features.persistence;

import java.util.List;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

@FunctionalInterface
public interface EventRepository<ID>  {
  List<UnitOfWork> eventsAfter(ID id, Version afterVersion);
}