package myeslib3.core.stack;

import lombok.Value;
import myeslib3.core.model.Event;

import java.util.List;

@Value
public class ProjectionData {

  String uowId;
  Long uowSequence;
  String targetId;
  List<Event> events;

}
