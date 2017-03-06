package myeslib3;

import com.spencerwi.either.Result;
import org.junit.jupiter.api.Test;

public class EitherTest {

  @Test
  public void accumulateErrors() {

    Result<Object> result = Result.attempt(() -> {throw new RuntimeException("1");})
            .flatMap(a -> {throw new RuntimeException("2");})
            .flatMap(b -> {throw new RuntimeException("3");});

    System.out.println(result.getException());

  }

}
