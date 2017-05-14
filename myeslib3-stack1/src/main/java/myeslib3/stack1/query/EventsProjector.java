package myeslib3.stack1.query;

import javaslang.collection.List;
import myeslib3.stack1.command.UnitOfWorkData;

public interface EventsProjector {

  String getEventsChannelId();

  Long getLastUowSeq();

  void handle(List<UnitOfWorkData> uowList);

}
