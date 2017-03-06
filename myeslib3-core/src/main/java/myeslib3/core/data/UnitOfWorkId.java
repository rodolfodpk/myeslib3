package myeslib3.core.data;

import java.util.UUID;
import lombok.Value;

@Value
public class UnitOfWorkId {

  public UUID uuid;

  public static UnitOfWorkId create() {
    return new UnitOfWorkId(UUID.randomUUID());
  }

  public static UnitOfWorkId create(UUID id) {
    return new UnitOfWorkId(id);
  }

}
