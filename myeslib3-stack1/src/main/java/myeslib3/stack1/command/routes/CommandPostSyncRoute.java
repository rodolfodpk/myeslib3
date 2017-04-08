package myeslib3.stack1.command.routes;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import myeslib3.core.StateTransitionsTracker;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Command;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.functions.CommandHandlerFn;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;
import myeslib3.stack1.command.CommandExecution;
import myeslib3.stack1.command.CommandExecutions;
import myeslib3.stack1.command.SnapshotReader;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.spi.IdempotentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static myeslib3.stack1.command.CommandExecutions.ERROR;
import static myeslib3.stack1.command.CommandExecutions.SUCCESS;
import static myeslib3.stack1.stack1infra.utils.StringHelper.*;

@AllArgsConstructor
public class CommandPostSyncRoute<A extends AggregateRoot, C extends Command> extends RouteBuilder {

	private static final String AGGREGATE_ROOT_ID = "aggregate_root_id";
  private static final String COMMAND_ID = "command_id";
  private static final String RESULT = "result";
  private static final String APPLICATION_JSON = "application/json";

	@NonNull final Class<A> aggregateRootClass;
	@NonNull final List<Class<?>> commandsClasses;
	@NonNull final CommandHandlerFn<A, C> handler;
	@NonNull final Supplier<A> supplier;
	@NonNull final DependencyInjectionFn<A> dependencyInjectionFn;
	@NonNull final StateTransitionFn<A> stateTransitionFn;
	@NonNull final SnapshotReader<A> snapshotReader;
	@NonNull final WriteModelRepository writeModelRepo;
	@NonNull final Gson gson ;
	@NonNull final IdempotentRepository<String> idempotentRepo;

	@Override
  public void configure() throws Exception {

    restConfiguration().component("undertow").bindingMode(RestBindingMode.auto)
            .dataFormatProperty("prettyPrint", "true")
            .contextPath("/").port(8080)
            .apiContextPath("/api-doc")
            .apiProperty("api.title", "Customer API").apiProperty("api.version", "1.0.0")
            .enableCORS(true);

    commandsClasses.forEach(this::createRouteForCommand);

    fromF("direct:save-events-%s", aggregateRootId(aggregateRootClass))
      .routeId("save-events-" + aggregateRootId(aggregateRootClass))
      .idempotentConsumer(header(COMMAND_ID)).messageIdRepository(idempotentRepo)
      .process(new SaveEventsProcessor())
      ;
  }

  private void createRouteForCommand(Class<?> commandClazz) {

    final GsonDataFormat df = new GsonDataFormat(gson, commandClazz);

    rest("/" + aggregateRootId(aggregateRootClass))
      .put("{" + AGGREGATE_ROOT_ID + "}/" + commandId(commandClazz) + "/{" + COMMAND_ID + "}")
            .id(aggrCmdRoot("put-", aggregateRootClass, commandClazz))
            .description("post a new " + commandId(commandClazz))
      .consumes(APPLICATION_JSON).type(commandClazz)
        .param()
          .name(AGGREGATE_ROOT_ID).description("the id of the target AggregateRoot instance")
          .type(RestParamType.query).dataType("java.util.String")
        .endParam()
        .param()
          .name(COMMAND_ID).description("the id of the requested functions")
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
        .to("direct:handle-" + commandId(commandClazz));

    fromF("direct:handle-%s", commandId(commandClazz))
      .routeId(aggrCmdRoot("handle-", aggregateRootClass, commandClazz))
 //     .streamCaching()
      .log("as gson: ${body}")
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
          .groupKey(commandClazz.getSimpleName())
          .executionTimeoutInMilliseconds(5000).circuitBreakerSleepWindowInMilliseconds(10000)
        .end()
        .process(new CommandProcessor())
        .log("result: ${header.RESULT} ")
        .toF("direct:save-events-%s", aggregateRootId(aggregateRootClass))
        .log("*** after save : ${body}")
      .onFallback()
        .transform().constant(Arrays.asList("fallback - circuit breaker seems to be open"))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
      .end()
      .marshal(df)
      .log("as gson result: ${body}")
    ;

  }

  final class CommandProcessor implements Processor {

    @Override
		public void process(Exchange e) throws Exception {

			final String targetId = e.getIn().getHeader(AGGREGATE_ROOT_ID, String.class);
			final String commandId = e.getIn().getHeader(COMMAND_ID, String.class);
			final Command command = e.getIn().getBody(Command.class);
			final C _command = (C) command;
			final StateTransitionsTracker<A> tracker = new StateTransitionsTracker<>(supplier.get(),
              stateTransitionFn, dependencyInjectionFn);
			final SnapshotReader.Snapshot<A> snapshot = snapshotReader.getSnapshot(targetId, tracker);
			final UnitOfWork unitOfWork;
			CommandExecution result;
			try {
				unitOfWork = handler.handle(_command,
								targetId, snapshot.getInstance(), snapshot.getVersion(),
                stateTransitionFn, dependencyInjectionFn);
				result = SUCCESS(unitOfWork);
			} catch (Exception ex) {
				result = ERROR(ex);
			}
      e.getOut().setHeader(COMMAND_ID, commandId);
      e.getOut().setBody(command, Command.class);
			e.getOut().setHeader(RESULT, result);

		}
	}

	final class SaveEventsProcessor implements Processor {

		@Override
		public void process(Exchange e) throws Exception {

      final String commandId = e.getIn().getHeader(COMMAND_ID, String.class);
			final Command command = e.getIn().getBody(Command.class);
			final C _command = (C) command;
			final CommandExecution result = e.getIn().getHeader(RESULT, CommandExecution.class);
			Runnable r = CommandExecutions.caseOf(result)
				.SUCCESS(uow -> (Runnable) () -> {
          writeModelRepo.append(uow, _command, command1 -> commandId);
					e.getOut().setBody(uow, UnitOfWork.class);
					e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
				})
				.ERROR(exception -> () -> {
					e.getOut().setBody(Arrays.asList(exception.getMessage()), List.class);
					e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
				});
      r.run();
		}
	}
}
