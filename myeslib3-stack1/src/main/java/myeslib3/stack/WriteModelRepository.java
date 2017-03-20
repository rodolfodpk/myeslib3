package myeslib3.stack;

import myeslib3.core.data.Command;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

import java.util.List;
import java.util.UUID;

public interface WriteModelRepository  {

	void append(UnitOfWork unitOfWork, Command causeCommand);

	UnitOfWork getUow(UUID uowId);

	List<UnitOfWork> getAll(String id);

	List<UnitOfWork> getAllAfterVersion(String id, Version version);

}
