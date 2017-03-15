package myeslib3.stack.persistence;

import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

import java.util.List;

@FunctionalInterface
public interface EventRepository  {
  List<UnitOfWork> eventsAfter(String id, Version afterVersion);
}
