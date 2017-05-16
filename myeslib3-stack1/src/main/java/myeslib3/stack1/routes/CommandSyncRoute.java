package myeslib3.stack1.routes;

import com.google.gson.Gson;
import javaslang.control.Either;
import lombok.NonNull;
import lombok.val;
import myeslib3.core.UnitOfWork;
import myeslib3.core.model.AggregateRoot;
import myeslib3.core.model.AggregateRootCmdHandler;
import myeslib3.core.model.AggregateRootId;
import myeslib3.core.model.Command;
import myeslib3.core.stack.Snapshot;
import myeslib3.core.stack.SnapshotReader;
import myeslib3.core.stack.WriteModelRepository;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.IdempotentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static myeslib3.stack1.routes.HeadersConstants.COMMAND_ID;
import static myeslib3.stack1.routes.StringHelper.aggregateRootId;

public class CommandSyncRoute<ID extends AggregateRootId, A extends AggregateRoot> extends RouteBuilder {

  static final String RESULT = "result";
  static final String IS_ERROR = "IS_ERROR";

  final Class<A> aggregateRootClass;
  final SnapshotReader<ID, A> snapshotReader;
  final AggregateRootCmdHandler<A> handler;
  final WriteModelRepository writeModelRepo;
  final Gson gson ;
  final IdempotentRepository<String> idempotentRepo;

  public CommandSyncRoute(@NonNull Class<A> aggregateRootClass, @NonNull SnapshotReader<ID, A> snapshotReader,
                          @NonNull AggregateRootCmdHandler<A> handler, @NonNull WriteModelRepository writeModelRepo,
                          @NonNull Gson gson, @NonNull IdempotentRepository<String> idempotentRepo) {
    this.aggregateRootClass = aggregateRootClass;
    this.snapshotReader = snapshotReader;
    this.handler = handler;
    this.writeModelRepo = writeModelRepo;
    this.gson = gson;
    this.idempotentRepo = idempotentRepo;
  }

  @Override
  public void configure() throws Exception {

    fromF("direct:handle-cmd-%s", aggregateRootId(aggregateRootClass))
      .routeId("handle-cmd-" + aggregateRootId(aggregateRootClass))
      .log("received command as json: ${body}")
      .doTry()
        .process(e -> {
          final String asJson = e.getIn().getBody(String.class);
          final Command instance = gson.fromJson(asJson, Command.class);
          e.getOut().setBody(instance, Command.class);
          e.getOut().setHeaders(e.getIn().getHeaders());
        })
        .log("command as java: ${body}")
      .doCatch(Exception.class)
        .log(LoggingLevel.ERROR, "error ")
        .setBody(constant(Arrays.asList("gson serialization error")))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
        .process(e -> {
          final List<String> instance = e.getIn().getBody(List.class);
          final String asJson = gson.toJson(instance, List.class);
          e.getOut().setBody(asJson, String.class);
        })
        .log(LoggingLevel.ERROR, "error as json: ${body}")
        .stop()
      .end()
      .hystrix()
        .hystrixConfiguration()
          .groupKey("handle-cmd-" + aggregateRootId(aggregateRootClass))
          .executionTimeoutInMilliseconds(5000).circuitBreakerSleepWindowInMilliseconds(10000)
        .end()
        .process(new CommandProcessor())
        .toF("direct:process-results-%s", aggregateRootId(aggregateRootClass))
      .onFallback()
        .setBody(constant(Arrays.asList("fallback - circuit breaker seems to be open")))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
        .process(e -> {
          final List instance = e.getIn().getBody(List.class);
          final String asJson = gson.toJson(instance, List.class);
          e.getOut().setBody(asJson, String.class);
          e.getOut().setHeaders(e.getIn().getHeaders());
        })
      .end()
      .log("result as json: ${body}")
    ;

    fromF("direct:process-results-%s", aggregateRootId(aggregateRootClass))
      .routeId("process-results-" + aggregateRootId(aggregateRootClass))
      .choice()
        .when(header(IS_ERROR).isEqualTo(false))
          .toF("direct:save-events-%s", aggregateRootId(aggregateRootClass))
          .process(e -> {
            final Either<Exception, Optional<UnitOfWork>> result = e.getIn().getHeader(RESULT, Either.class);
            if (result.get().isPresent()) {
              val asJson = gson.toJson(result.get().get());
              e.getOut().setBody(asJson, String.class);
              e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
            } else {
              e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            }
          })
        .otherwise()
          .process(e -> {
            final Either<Exception, Optional<UnitOfWork>> result = e.getIn().getHeader(RESULT, Either.class);
            val asJson = gson.toJson(Arrays.asList(result.getLeft().getMessage()), List.class);
            e.getOut().setBody(asJson);
            e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
          })
      .end()
    ;

    fromF("direct:save-events-%s", aggregateRootId(aggregateRootClass))
      .routeId("save-events-" + aggregateRootId(aggregateRootClass))
      .log(LoggingLevel.DEBUG, "${header.command_id}")
      .idempotentConsumer(header(COMMAND_ID)).messageIdRepository(idempotentRepo)
      .process(new SaveEventsProcessor())
      ;
  }

  final class CommandProcessor implements Processor {

    @Override
    public void process(Exchange e) throws Exception {

      final Command command = e.getIn().getBody(Command.class);
      final Snapshot<A> snapshot = snapshotReader.getSnapshot((ID) command.getTargetId());
      Either<Exception, Optional<UnitOfWork>> result ;
      try {
        result = Either.right(handler.handle(command, snapshot.getInstance(), snapshot.getVersion()));
      } catch (Exception ex) {
        result = Either.left(ex);
      }
      e.getOut().setBody(command, Command.class);
      e.getOut().setHeader(IS_ERROR, result.isLeft());
      e.getOut().setHeader(COMMAND_ID, command.getCommandId());
      e.getOut().setHeader(RESULT, result);
    }
  }

  final class SaveEventsProcessor implements Processor {

    @Override
    public void process(Exchange e) throws Exception {

      final Either<Exception, Optional<UnitOfWork>> result = e.getIn().getHeader(RESULT, Either.class);

      if (result.get().isPresent()) {
          writeModelRepo.append(result.get().get());
        }
      }

  }

}
