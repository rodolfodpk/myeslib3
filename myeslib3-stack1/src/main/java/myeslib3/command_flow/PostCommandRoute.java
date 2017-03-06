package myeslib3.command_flow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spencerwi.either.Result;
import java.util.List;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Command;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.functions.CommandHandlerFn;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;
import static myeslib3.helpers.StringHelpers.aggregateRootId;
import static myeslib3.helpers.StringHelpers.commandId;
import myeslib3.persistence.SnapshotReader;
import myeslib3.persistence.SnapshotReader.Snapshot;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
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

  final Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory()).create();

  public PostCommandRoute(Class<A> aggregateRootClass,
                          List<Class<?>> commandsClasses,
                          CommandHandlerFn<A, C> handler,
                          StateTransitionFn<A, Event> stateTransitionFn,
                          DependencyInjectionFn<A> dependencyInjectionFn,
                          SnapshotReader<String, A> snapshotReader) {
    this.aggregateRootClass = aggregateRootClass;
    this.commandsClasses = commandsClasses;
    this.handler = handler;
    this.stateTransitionFn = stateTransitionFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
    this.snapshotReader = snapshotReader;
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
          .log("as json: ${body}")
          .doTry()
              .unmarshal(df)
              .log("as java: ${body}")
          .doCatch(Exception.class)
              .log("*** error ")
              .setBody(constant(ErrorMessage.create("1", "json serialization error")))
              .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
              .marshal().json(JsonLibrary.Gson, ErrorMessage.class)
              .log("as json error: ${body}")
              .stop()
          .end()
          .process(e -> {
            final String aggreateRootId = e.getIn().getHeader(AGGREGATE_ROOT_ID, String.class);
            final String commandId = e.getIn().getHeader(COMMAND_ID, String.class);
            final Command command = (Command) e.getIn().getBody(commandClazz);
            final Snapshot<A> snapshot = snapshotReader.getSnapshot(aggreateRootId);
            final Result<UnitOfWork> result = handler.handle(commandId, aggreateRootId,
                    snapshot.getInstance(), snapshot.getVersion(), (C) command,
                    stateTransitionFn, dependencyInjectionFn);
            if (result.isOk()){
              e.getOut().setBody(result.getResult(), UnitOfWork.class);
              e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
            } else {
              e.getOut().setBody(ErrorMessage.create("1", result.getException().getMessage()));
              e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            }
          })
          .marshal(df)
          .log("as json result: ${body}")
        ;

      });
  }

}
