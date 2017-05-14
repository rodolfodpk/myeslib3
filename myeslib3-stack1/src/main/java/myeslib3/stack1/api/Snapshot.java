package myeslib3.stack1.api;

import lombok.Value;
import myeslib3.core.data.Version;

@Value
public class Snapshot<A> {

  final A instance;
  final Version version;

}
