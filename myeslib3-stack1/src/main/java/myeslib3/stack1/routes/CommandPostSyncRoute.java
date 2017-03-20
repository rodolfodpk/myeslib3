package myeslib3.stack1.routes;

import com.google.gson.Gson;
import com.spencerwi.either.Result;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import myeslib3.core.StateTransitionsTracker;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Command;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.functions.CommandHandlerFn;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;
import myeslib3.stack.SnapshotReader;
import myeslib3.stack.WriteModelRepository;
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

import static myeslib3.stack1.infra.utils.StringHelper.aggregateRootId;
import static myeslib3.stack1.infra.utils.StringHelper.commandId;

@AllArgsConstructor
public class CommandPostSyncRoute<A extends AggregateRoot, C extends Command> extends RouteBuilder {

	private static final String AGGREGATE_ROOT_ID = "aggregate_root_id";
  private static final String COMMAND_ID = "command_id";
	static final String APPLICATION_JSON = "application/json";

	@NonNull Class<A> aggregateRootClass;
	@NonNull List<Class<?>> commandsClasses;
	@NonNull CommandHandlerFn<A, C> handler;
	@NonNull Supplier<A> supplier;
	@NonNull DependencyInjectionFn<A> dependencyInjectionFn;
	@NonNull StateTransitionFn<A> stateTransitionFn;
	@NonNull SnapshotReader<A> snapshotReader;
	@NonNull WriteModelRepository writeModelRepo;
	@NonNull Gson gson ;
	@NonNull IdempotentRepository<String> idempotentRepo;

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
          .consumes(APPLICATION_JSON).type(commandClazz)
            .param()
              .name(AGGREGATE_ROOT_ID).description("the id of the target AggregateRoot instance")
              .type(RestParamType.query).dataType("java.util.String")
            .endParam()
            .param()
              .name(COMMAND_ID).description("the id of the requested command")
              .type(RestParamType.query).dataType("String")
            .endParam()
          .produces(APPLICATION_JSON)
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
                writeModelRepo.append(result.getResult(), _command);
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
