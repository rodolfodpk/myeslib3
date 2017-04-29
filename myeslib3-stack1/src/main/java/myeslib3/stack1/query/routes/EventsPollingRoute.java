package myeslib3.stack1.query.routes;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.stack1infra.BoundedContextConfig;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;

import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.camel.builder.PredicateBuilder.not;

public class EventsPollingRoute extends RouteBuilder {

	final String eventsChannelId;
	final WriteModelRepository repo;
	final BoundedContextConfig config;
	@Getter
	final AtomicInteger failures = new AtomicInteger();
  @Getter
	final AtomicInteger idles = new AtomicInteger();
  @Getter
  final AtomicInteger backoffCount = new AtomicInteger();

  static final String RESULT_SIZE_HEADER = "RESULT_SIZE_HEADER";

	public EventsPollingRoute(@NonNull String eventsChannelId, @NonNull WriteModelRepository repo,
                            @NonNull BoundedContextConfig config) {
		this.eventsChannelId = eventsChannelId;
		this.repo = repo;
    this.config = config;
  }

	@Override
	public void configure() throws Exception {

    final Predicate hasReachedAnyThreshold = exchange -> failures.get() >= config.events_backoff_failures_threshold() ||
            idles.get() >= config.events_backoff_iddle_threshold();

    final Predicate backoffCountBiggerThanZero = exchange -> backoffCount.get() > 0;

    fromF("direct:pool-events-%s", eventsChannelId)
      .routeId("pool-events-" + eventsChannelId)
      .log("before -> ${body}")
      .choice()
        .when(hasReachedAnyThreshold)
          .toF("direct:pool-events-open-cb-%s", eventsChannelId)
      .end()
      .doTry()
        .toF("direct:pool-events-perform-%s", eventsChannelId)
      .doCatch(Throwable.class)
        .setHeader("msg", method(this, "getFailures().incrementAndGet()"))
        .log("Failure pooling operations incremented to ${header.msg}")
      .end()
      .log("after -> ${body}")
    ;

    fromF("direct:pool-events-open-cb-%s", eventsChannelId)
      .routeId("pool-events-open-cb-" + eventsChannelId)
      .process(e -> {
        failures.set(0); idles.set(0);
        backoffCount.updateAndGet(operand -> operand + config.events_backoff_multiplier());
      })
    .log("circuit breaker is now open")
    ;

    fromF("direct:pool-events-perform-%s", eventsChannelId)
      .routeId("pool-events-perform-" + eventsChannelId)
      .errorHandler(noErrorHandler())
      .choice()
        .when(backoffCountBiggerThanZero)
          .setHeader("msg", method(this, "getBackoffCount().decrementAndGet()"))
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
        val unitsOfWork = repo.getAllSince(repo.getLastUowSequence(), config.events_max_rows_query());
        e.getOut().setHeader(RESULT_SIZE_HEADER, unitsOfWork.size());
        e.getOut().setBody(unitsOfWork);
      })
      .log("--> ${body}")
      .choice()
        .when(header(RESULT_SIZE_HEADER).isEqualTo(0))
            .setHeader("msg", method(this, "getIdles().incrementAndGet()"))
            .log("Failure pooling operations incremented to ${header.msg}")
        .otherwise()
          .log("Found ${header.RESULT_SIZE_HEADER} units of work to project")
          .toF("seda:%s-events", eventsChannelId)
      .end()
    ;

  }
}
