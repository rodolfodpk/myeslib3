package myeslib3.example1.projections;

import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
public class CustomerSummary {
  String id;
  int activationsCount;
}
