package myeslib3.core.stack;

import lombok.Value;
import myeslib3.core.Version;
import myeslib3.core.model.Event;

import java.util.List;

@Value
public class VersionedEvents {

  Version version;
  List<Event> events;

}
