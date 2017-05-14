package myeslib3.stack1.query.routes;

import javaslang.collection.List;
import lombok.NonNull;
import myeslib3.stack1.command.UnitOfWorkData;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.query.EventsProjector;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;

import javax.inject.Named;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.camel.builder.PredicateBuilder.not;

public class EventsPollingRoute extends RouteBuilder {

	final WriteModelRepository repo;
	final EventsProjector eventsProjectorDao;
	final int eventsBackoffFailuresThreshold;
  final int eventsBackoffIddlesThreshold;
  final int eventsBackoffMultiplier;
  final int eventsMaxRowsPerPooling;
	final AtomicInteger failures = new AtomicInteger();
	final AtomicInteger idles = new AtomicInteger();
  final AtomicInteger backoffCount = new AtomicInteger();

  static final String RESULT_SIZE_HEADER = "RESULT_SIZE_HEADER";

	public EventsPollingRoute(@NonNull WriteModelRepository repo,
                            @NonNull EventsProjector eventsProjectorDao,
                            @NonNull @Named("events_backoff_failure_threshold") int eventsBackoffFailureThreshold,
                            @NonNull @Named("events_backoff_idle_threshold") int eventsBackoffIdleThreshold,
                            @NonNull @Named("events_backoff_multiplier") int eventsBackoffMultiplier,
                            @NonNull @Named("events_max_rows_query") int eventsMaxRowsPerPooling) {
		this.repo = repo;
    this.eventsProjectorDao = eventsProjectorDao;
    this.eventsBackoffFailuresThreshold = eventsBackoffFailureThreshold;
    this.eventsBackoffIddlesThreshold = eventsBackoffIdleThreshold;
    this.eventsBackoffMultiplier = eventsBackoffMultiplier;
    this.eventsMaxRowsPerPooling = eventsMaxRowsPerPooling;
  }

	@Override
	public void configure() throws Exception {

    final Predicate hasReachedAnyThreshold = exchange -> failures.get() >= eventsBackoffFailuresThreshold ||
            idles.get() >= eventsBackoffIddlesThreshold;

    final Predicate backoffCountBiggerThanZero = exchange -> backoffCount.get() > 0;

    fromF("direct:pool-events-%s", eventsProjectorDao.getEventsChannelId())
      .routeId("pool-events-" + eventsProjectorDao.getEventsChannelId())
      .log("before -> ${body}")
      .choice()
        .when(hasReachedAnyThreshold)
          .toF("direct:pool-events-open-cb-%s", eventsProjectorDao.getEventsChannelId())
      .end()
      .doTry()
        .toF("direct:pool-events-perform-%s", eventsProjectorDao.getEventsChannelId())
      .doCatch(Throwable.class)
        .setHeader("msg", method(this, "newFailure()"))
        .log("Failure pooling operations incremented to ${header.msg}")
      .end()
      .log("after -> ${body}")
    ;

    fromF("direct:pool-events-open-cb-%s", eventsProjectorDao.getEventsChannelId())
      .routeId("pool-events-open-cb-" + eventsProjectorDao.getEventsChannelId())
      .process(e -> {
        failures.set(0); idles.set(0);
        backoffCount.updateAndGet(operand -> operand + eventsBackoffMultiplier);
      })
    .log("circuit breaker is now open")
    ;

    fromF("direct:pool-events-perform-%s", eventsProjectorDao.getEventsChannelId())
      .routeId("pool-events-perform-" + eventsProjectorDao.getEventsChannelId())
      .errorHandler(noErrorHandler())
      .choice()
        .when(backoffCountBiggerThanZero)
          .setHeader("msg", method(this, "ranBackoff()"))
          .log("backoffCount was decremented to ${header.msg}")
            .choice()
              .when(not(backoffCountBiggerThanZero))
                .log("circuit breaker is now off")
                .stop()
            .otherwise()
                .stop()
            .endChoice()
        .endChoice()
      .end()
      .log("--> will pool")
      .process(e -> {
        final List<UnitOfWorkData> unitsOfWork =
                repo.getAllSince(eventsProjectorDao.getLastUowSeq(), eventsMaxRowsPerPooling);
        e.getOut().setHeader(RESULT_SIZE_HEADER, unitsOfWork.size());
        e.getOut().setBody(unitsOfWork);
      })
      .log("--> ${body}")
      .choice()
        .when(header(RESULT_SIZE_HEADER).isEqualTo(0))
          .setHeader("msg", method(this, "newIdle()"))
          .log("Failure pooling operations incremented to ${header.msg}")
        .otherwise()
          .log("Found ${header.RESULT_SIZE_HEADER} units of work to project")
          .split(body())
          .toF("seda:%s-events", eventsProjectorDao.getEventsChannelId())
      .end()
    ;

  }

  public Integer newIdle() {
	  return idles.incrementAndGet();
  }

  public Integer newFailure() {
    return failures.incrementAndGet();
  }

  public Integer ranBackoff() {
    return backoffCount.decrementAndGet();
  }

}
