package myeslib3.stack;

import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

import java.util.List;

public interface WriteModelDao {

	  void append(UnitOfWork unitOfWork);

    List<UnitOfWork> getAll(String id);

    List<UnitOfWork> getAllAfterVersion(String id, Version version);

}
