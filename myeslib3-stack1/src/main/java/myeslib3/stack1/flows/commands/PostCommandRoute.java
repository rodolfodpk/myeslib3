package myeslib3.stack1.flows.commands;

import com.google.gson.Gson;
import com.spencerwi.either.Result;
import java.util.Arrays;
import java.util.List;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Command;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.functions.CommandHandlerFn;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;
import static myeslib3.stack1.utils.StringHelpers.aggregateRootId;
import static myeslib3.stack1.utils.StringHelpers.commandId;
import myeslib3.stack1.features.persistence.SnapshotReader;
import myeslib3.stack1.features.persistence.SnapshotReader.Snapshot;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;

public class PostCommandRoute<A extends AggregateRoot, C extends Command> extends RouteBuilder {

  public static final String AGGREGATE_ROOT_ID = "aggregate_root_id";
  public static final String COMMAND_ID = "command_id";
  final Class<A> aggregateRootClass;
  final List<Class<?>> commandsClasses;
  final CommandHandlerFn<A, C> handler;
  final StateTransitionFn<A, Event> stateTransitionFn;
  final DependencyInjectionFn<A> dependencyInjectionFn;
  final SnapshotReader<String, A> snapshotReader;
  final Gson gson ;

  //  Journal<String> journal;


  public PostCommandRoute(Class<A> aggregateRootClass,
                          List<Class<?>> commandsClasses,
                          CommandHandlerFn<A, C> handler,
                          StateTransitionFn<A, Event> stateTransitionFn,
                          DependencyInjectionFn<A> dependencyInjectionFn,
                          SnapshotReader<String, A> snapshotReader,
                          Gson gson) {
    this.aggregateRootClass = aggregateRootClass;
    this.commandsClasses = commandsClasses;
    this.handler = handler;
    this.stateTransitionFn = stateTransitionFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
    this.snapshotReader = snapshotReader;
    this.gson = gson;
  }

  @Override
  public void configure() throws Exception {

    restConfiguration().component("jetty").bindingMode(RestBindingMode.auto)
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
          .consumes("application/json").type(commandClazz)
          .produces("application/json")
          .route()
          .routeId("put-" + commandId(commandClazz))
          .streamCaching()
          .log("as json: ${body}")
          .doTry()
              .unmarshal(df)
              .log("as java: ${body}")
          .doCatch(Exception.class)
              .log("*** error ")
              .setBody(constant(Arrays.asList("json serialization error")))
              .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
              .marshal().json(JsonLibrary.Gson, List.class)
              .log("as json error: ${body}")
              .stop()
          .end()
          .hystrix()
            .hystrixConfiguration()
              .executionTimeoutInMilliseconds(5000).circuitBreakerSleepWindowInMilliseconds(10000)
            .end()
            .process(e -> {
              final String aggregateRootId = e.getIn().getHeader(AGGREGATE_ROOT_ID, String.class);
              final String commandId = e.getIn().getHeader(COMMAND_ID, String.class);
              final Command command = e.getIn().getBody(Command.class);
              final C _command = (C) command;
              final Snapshot<A> snapshot = snapshotReader.getSnapshot(aggregateRootId);
              final Result<UnitOfWork> result = handler.handle(commandId, aggregateRootId, _command,
                      snapshot.getInstance(), snapshot.getVersion(),
                      stateTransitionFn, dependencyInjectionFn);
              if (result.isOk()){
                // journal.append(aggreateRootId, result.getResult());
                e.getOut().setBody(result.getResult(), UnitOfWork.class);
                e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
              } else {
                e.getOut().setBody(Arrays.asList(result.getException().getMessage()), List.class);
                e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
              }
            })
          .onFallback()
            .transform().constant(Arrays.asList("fallback - circuit breaker seems to be open"))
          .end()
          .marshal(df)
          .log("as json result: ${body}")
        ;

      });
  }

}
