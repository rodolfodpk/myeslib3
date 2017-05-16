package myeslib3.core.stack;

import lombok.Value;
import myeslib3.core.Version;

@Value
public class Snapshot<A> {

  final A instance;
  final Version version;

}
