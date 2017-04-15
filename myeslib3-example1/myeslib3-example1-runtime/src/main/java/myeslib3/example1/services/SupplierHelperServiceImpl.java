package myeslib3.example1.services;

import java.time.LocalDateTime;
import java.util.UUID;

public class SupplierHelperServiceImpl implements SupplierHelperService {

  public UUID uuid() {
    return UUID.randomUUID();
  }

  public LocalDateTime now() {
    return LocalDateTime.now();
  }

}
