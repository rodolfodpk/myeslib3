package myeslib3.stack1.command.routes;

import com.google.gson.Gson;
import javaslang.control.Either;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import myeslib3.core.AggregateRootCmdHandler;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.AggregateRootId;
import myeslib3.core.data.Command;
import myeslib3.core.data.UnitOfWork;
import myeslib3.stack1.command.Snapshot;
import myeslib3.stack1.command.SnapshotReader;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.IdempotentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static myeslib3.stack1.Headers.COMMAND_ID;
import static myeslib3.stack1.stack1infra.utils.StringHelper.aggregateRootId;

@AllArgsConstructor
public class CommandSyncRoute<ID extends AggregateRootId, A extends AggregateRoot> extends RouteBuilder {

  static final String RESULT = "result";

  @NonNull final Class<A> aggregateRootClass;
  @NonNull final SnapshotReader<ID, A> snapshotReader;
  @NonNull final AggregateRootCmdHandler<A> handler;
  @NonNull final WriteModelRepository<ID> writeModelRepo;
  @NonNull final Gson gson ;
  @NonNull final IdempotentRepository<String> idempotentRepo;

  @Override
  public void configure() throws Exception {

    fromF("direct:handle-cmd-%s", aggregateRootId(aggregateRootClass))
      .routeId("handle-cmd-" + aggregateRootId(aggregateRootClass))
      .log("as json: ${body}")
      .doTry()
        .process(e -> {
          final String asJson = e.getIn().getBody(String.class);
          final Command instance = gson.fromJson(asJson, Command.class);
          e.getOut().setBody(instance, Command.class);
          e.getOut().setHeaders(e.getIn().getHeaders());
        })
        .log("as java: ${body}")
      .doCatch(Exception.class)
        .log("*** error ")
        .setBody(constant(Arrays.asList("gson serialization error")))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
        .process(e -> {
          final List<String> instance = e.getIn().getBody(List.class);
          final String asJson = gson.toJson(instance, List.class);
          e.getOut().setBody(asJson, String.class);
        })
        .log("as json error: ${body}")
        .stop()
      .end()
      .hystrix()
        .hystrixConfiguration()
          .groupKey("handle-cmd-" + aggregateRootId(aggregateRootClass))
          .executionTimeoutInMilliseconds(5000).circuitBreakerSleepWindowInMilliseconds(10000)
        .end()
        .process(new CommandProcessor())
        .toF("direct:save-events-%s", aggregateRootId(aggregateRootClass))
        .log("*** after save : ${body}")
        .process(e -> {
          final UnitOfWork instance = e.getIn().getBody(UnitOfWork.class);
          final String asJson = gson.toJson(instance, UnitOfWork.class);
          e.getOut().setBody(asJson, String.class);
          e.getOut().setHeaders(e.getIn().getHeaders());
        })
      .onFallback()
        .transform().constant(Arrays.asList("fallback - circuit breaker seems to be open"))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
        .process(e -> {
          final List instance = e.getIn().getBody(List.class);
          final String asJson = gson.toJson(instance, List.class);
          e.getOut().setBody(asJson, String.class);
          e.getOut().setHeaders(e.getIn().getHeaders());
        })
      .end()
      .log("UnitOfWork as json result: ${body}")
    ;

    fromF("direct:save-events-%s", aggregateRootId(aggregateRootClass))
      .routeId("save-events-" + aggregateRootId(aggregateRootClass))
      .log("${header.command_id}")
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
      e.getOut().setHeaders(e.getIn().getHeaders());
      e.getOut().setHeader(RESULT, result);
    }
  }

  final class SaveEventsProcessor implements Processor {

    @Override
    public void process(Exchange e) throws Exception {

      final Either<Exception, Optional<UnitOfWork>> result = e.getIn().getHeader(RESULT, Either.class);

      if (result.isRight()) {
        if (result.get().isPresent()) {
          writeModelRepo.append(result.get().get());
          e.getOut().setBody(result.get().get(), UnitOfWork.class);
          e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
        } else {
          e.getOut().setBody(Arrays.asList("unknown command"), List.class);
          e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
        }
      } else {
        e.getOut().setBody(Arrays.asList(result.getLeft().getMessage()), List.class);
        e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
      }

    }
  }

}
