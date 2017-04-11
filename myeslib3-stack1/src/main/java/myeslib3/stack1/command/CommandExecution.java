package myeslib3.stack1.command;


import myeslib3.core.data.UnitOfWork;
import org.derive4j.ArgOption;
import org.derive4j.Data;

import java.util.Optional;

@Data(arguments = ArgOption.checkedNotNull)
public abstract class CommandExecution {

	interface Cases<C> {
		C SUCCESS(Optional<UnitOfWork> unitOfWork);
		C ERROR(Exception exception);
	}

	public abstract <C> C match(Cases<C> cases);

}