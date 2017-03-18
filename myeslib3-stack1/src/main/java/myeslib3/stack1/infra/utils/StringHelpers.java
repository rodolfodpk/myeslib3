package myeslib3.stack1.infra.utils;

import myeslib3.core.data.AggregateRoot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelpers {

  public static String aggregateRootId(Class<? extends AggregateRoot> aggregateRootClass) {
    return camelCaseToSnakeCase(aggregateRootClass.getSimpleName());
  }

  public static String commandId(Class<?> commandClass) {
    return camelCaseToSnakeCase(commandClass.getSimpleName());
  }

  public static String camelCaseToSnakeCase(String start) {
    Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(start);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      m.appendReplacement(sb, "_"+m.group());
    }
    m.appendTail(sb);
    return sb.toString().toLowerCase();
  }

  public static String SnakeCaseToCamelCase(String start) {
    StringBuffer sb = new StringBuffer();
    for (String s : start.split("_")) {
      sb.append(Character.toUpperCase(s.charAt(0)));
      if (s.length() > 1) {
        sb.append(s.substring(1, s.length()).toLowerCase());
      }
    }
    return sb.toString().toLowerCase();
  }

  public static void main(String... args) throws NoSuchAlgorithmException {
    int size = 1024 * 1024;
    byte[] bytes = new byte[size];
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    long startTime = System.nanoTime();
    for (int i = 0; i < 1024; i++)
      md.update(bytes, 0, size);
    long endTime = System.nanoTime();
    System.out.println(String.format("%1$064x", new java.math.BigInteger(1, md.digest())));
    System.out.println(String.format("%d ms", (endTime - startTime) / 1000000));
  }

}
