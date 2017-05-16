package myeslib3.core.model;

import myeslib3.core.stack.ProjectionData;

import java.util.List;

public interface EventsProjector {

  String getEventsChannelId();

  Long getLastUowSeq();

  void handle(List<ProjectionData> uowList);

}
