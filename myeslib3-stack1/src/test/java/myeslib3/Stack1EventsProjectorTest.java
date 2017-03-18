package myeslib3;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Stack1EventsProjectorTest {

  MessageDigest md ;
  {
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  String digest(String aggregateRootId) {
    try {
      md.update(aggregateRootId.getBytes("UTF-8"), 0, 2);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    byte[] digest = md.digest();
    StringBuffer sb = new StringBuffer();
    for (byte b : digest)
      sb.append(String.format("%02x", b & 0xff));
    return sb.toString();
  }

  @Test
  void test1() {
    System.out.println(digest("customer9"));
    System.out.println(digest("customer9"));
    System.out.println(digest("customer1"));
    System.out.println(digest("customer0"));
    System.out.println(digest("tereco----"));
    System.out.println(digest("fuba----"));
  }

}
