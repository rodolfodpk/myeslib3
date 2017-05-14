package myeslib3.stack1.api;

import javaslang.collection.List;

public interface EventsProjector {

  String getEventsChannelId();

  Long getLastUowSeq();

  void handle(List<UnitOfWorkData> uowList);

}
