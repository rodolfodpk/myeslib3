package myeslib3.stack1.utils;

import javaslang.Tuple2;
import javaslang.collection.List;
import myeslib3.core.data.Event;
import myeslib3.core.data.Version;

public class EventsHelper {

//	public static List<Event> flatMap(final List<UnitOfWork> unitOfWorks) {
//		return unitOfWorks.stream().flatMap((unitOfWork) -> unitOfWork.getEvents().stream()).collect(Collectors.toList());
//	}

	public static Version lastVersion(final Tuple2<Version, List<Event>> unitOfWorks) {
		return unitOfWorks._2().isEmpty() ? Version.create(0L) : unitOfWorks._1();
	}

}
