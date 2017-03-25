package myeslib3.stack1.stack1infra.utils;

import myeslib3.core.Event;
import myeslib3.core.UnitOfWork;
import myeslib3.core.Version;

import java.util.List;
import java.util.stream.Collectors;

public class EventsHelper {

	public static List<Event> flatMap(final List<UnitOfWork> unitOfWorks) {
		return unitOfWorks.stream().flatMap((unitOfWork) -> unitOfWork.getEvents().stream()).collect(Collectors.toList());
	}

	public static Version lastVersion(List<UnitOfWork> unitOfWorks) {
		return unitOfWorks.isEmpty() ? Version.create(0L) : unitOfWorks.get(unitOfWorks.size() - 1).getVersion();
	}

}
