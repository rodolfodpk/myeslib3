package myeslib3.command_flow;


import java.util.HashMap;
import java.util.Map;
import lombok.Value;

@Value
public class ErrorMessage {

  final Map<String, String> errors;

  public static ErrorMessage create(String key1, String value1) {
    final Map<String, String> map = new HashMap<>();
    map.put(key1, value1);
    return new ErrorMessage(map);
  }
}
