package myeslib3.stack1.command;

import javaslang.collection.List;
import lombok.Value;
import myeslib3.core.data.Event;

@Value
public class UnitOfWorkData {
  String uowId;
  Long uowSequence;
  String targetId;
  List<Event> events;
}
