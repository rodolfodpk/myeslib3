package myeslib3.stack1.routes;

import com.google.gson.Gson;
import com.spencerwi.either.Result;
import myeslib3.core.StateTransitionsTracker;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Command;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.functions.CommandHandlerFn;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;
import myeslib3.stack.SnapshotReader;
import myeslib3.stack.WriteModelDao;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.spi.IdempotentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static myeslib3.stack1.infra.utils.StringHelpers.aggregateRootId;
import static myeslib3.stack1.infra.utils.StringHelpers.commandId;

public class CommandPostSyncRoute<A extends AggregateRoot, C extends Command> extends RouteBuilder {

  private static final String AGGREGATE_ROOT_ID = "aggregate_root_id";
  private static final String COMMAND_ID = "command_id";

  final Class<A> aggregateRootClass;
  final List<Class<?>> commandsClasses;
  final CommandHandlerFn<A, C> handler;
  final StateTransitionFn<A> stateTransitionFn;
  final Supplier<A> supplier;
  final DependencyInjectionFn<A> dependencyInjectionFn;
  final SnapshotReader<A> snapshotReader;
  final WriteModelDao writeModelRepo;
  final Gson gson ;
  final IdempotentRepository<String> idempotentRepo;

  public CommandPostSyncRoute(Class<A> aggregateRootClass,
                              List<Class<?>> commandsClasses,
                              CommandHandlerFn<A, C> handler,
                              StateTransitionFn<A> stateTransitionFn,
                              Supplier<A> supplier,
                              DependencyInjectionFn<A> dependencyInjectionFn,
                              SnapshotReader<A> snapshotReader,
                              WriteModelDao writeModelRepo,
                              Gson gson,
                              IdempotentRepository<String> idempotentRepo) {
    this.aggregateRootClass = aggregateRootClass;
    this.commandsClasses = commandsClasses;
    this.handler = handler;
    this.stateTransitionFn = stateTransitionFn;
    this.supplier = supplier;
    this.dependencyInjectionFn = dependencyInjectionFn;
    this.snapshotReader = snapshotReader;
    this.writeModelRepo = writeModelRepo;
    this.gson = gson;
    this.idempotentRepo = idempotentRepo;
  }

  @Override
  public void configure() throws Exception {

    restConfiguration().component("undertow").bindingMode(RestBindingMode.auto)
            .dataFormatProperty("prettyPrint", "true")
            .contextPath("/").port(8080)
            .apiContextPath("/api-doc")
            .apiProperty("api.title", "**Bounded context** Example API").apiProperty("api.version", "1.0.0")
            .enableCORS(true);

      commandsClasses.forEach( commandClazz -> {

        GsonDataFormat df = new GsonDataFormat(gson, commandClazz);

        rest("/" + aggregateRootId(aggregateRootClass))
          .id("put-" + commandId(commandClazz))
          .put("{" + AGGREGATE_ROOT_ID + "}/" + commandId(commandClazz) + "/{" + COMMAND_ID + "}")
                .description("post a new " + commandId(commandClazz))
          .consumes("application/gson").type(commandClazz)
            .param()
              .name(AGGREGATE_ROOT_ID).description("the id of the target AggregateRoot instance")
              .type(RestParamType.query).dataType("java.util.String")
            .endParam()
            .param()
              .name(COMMAND_ID).description("the id of the requested command")
              .type(RestParamType.query).dataType("String")
            .endParam()
          .produces("application/gson")
            .responseMessage()
              .code(201).responseModel(UnitOfWork.class).message("created")
            .endResponseMessage()
            .responseMessage()
              .code(400).responseModel(List.class).message("bad request")
            .endResponseMessage()
            .responseMessage()
              .code(503).responseModel(List.class).message("service unavailable")
            .endResponseMessage()
          .route()
          .routeId("put-" + commandId(commandClazz))
     //     .streamCaching()
          .log("as gson: ${body}")
     //     .idempotentConsumer(header(AGGREGATE_ROOT_ID)).messageIdRepository(idempotentRepo)
          .doTry()
              .unmarshal(df)
              .log("as java: ${body}")
          .doCatch(Exception.class)
              .log("*** error ")
              .setBody(constant(Arrays.asList("gson serialization error")))
              .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
              .marshal().json(JsonLibrary.Gson, List.class)
              .log("as gson error: ${body}")
              .stop()
          .end()
          .hystrix()
            .hystrixConfiguration()
              .executionTimeoutInMilliseconds(5000).circuitBreakerSleepWindowInMilliseconds(10000)
            .end()
            .process(e -> {
              final String targetId = e.getIn().getHeader(AGGREGATE_ROOT_ID, String.class);
              final String commandId = e.getIn().getHeader(COMMAND_ID, String.class);
              final Command command = e.getIn().getBody(Command.class);
              final C _command = (C) command;
              final StateTransitionsTracker<A> tracker = new StateTransitionsTracker<A>(supplier.get(),
                      stateTransitionFn, dependencyInjectionFn);
              final SnapshotReader.Snapshot<A> snapshot = snapshotReader.getSnapshot(targetId, tracker);
              final Result<UnitOfWork> result = handler.handle(commandId, _command,
                      targetId, snapshot.getInstance(), snapshot.getVersion(),
                      stateTransitionFn, dependencyInjectionFn);
              if (result.isOk()){
                writeModelRepo.append(result.getResult());
                e.getOut().setBody(result.getResult(), UnitOfWork.class);
                e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
              } else {
                e.getOut().setBody(Arrays.asList(result.getException().getMessage()), List.class);
                e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
              }
            })
          .onFallback()
            .transform().constant(Arrays.asList("fallback - circuit breaker seems to be open"))
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
          .end()
          .marshal(df)
          .log("as gson result: ${body}")
        ;

      });
  }

}
