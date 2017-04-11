package myeslib3.stack1.command.routes;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.NonNull;
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
import org.apache.camel.spi.IdempotentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static myeslib3.stack1.command.CommandExecutions.ERROR;
import static myeslib3.stack1.command.CommandExecutions.SUCCESS;
import static myeslib3.stack1.stack1infra.utils.StringHelper.*;

@AllArgsConstructor
public class CommandSyncRoute<A extends AggregateRoot, C extends Command> extends RouteBuilder {

  static final String AGGREGATE_ROOT_ID = "aggregate_root_id";
  static final String COMMAND_ID = "command_id";
  static final String RESULT = "result";

  @NonNull final Class<A> aggregateRootClass;
  @NonNull final List<Class<?>> commandsClasses;
  @NonNull final CommandHandlerFn<A, C> handler;
  @NonNull final DependencyInjectionFn<A> dependencyInjectionFn;
  @NonNull final StateTransitionFn<A> stateTransitionFn;
  @NonNull final SnapshotReader<A> snapshotReader;
  @NonNull final WriteModelRepository writeModelRepo;
  @NonNull final Gson gson ;
  @NonNull final IdempotentRepository<String> idempotentRepo;

  @Override
  public void configure() throws Exception {

    commandsClasses.forEach(this::createRouteForCommand);

    fromF("direct:save-events-%s", aggregateRootId(aggregateRootClass))
      .routeId("save-events-" + aggregateRootId(aggregateRootClass))
      .log("${header.command_id}")
      .idempotentConsumer(header(COMMAND_ID)).messageIdRepository(idempotentRepo)
      .process(new SaveEventsProcessor())
      ;
  }

  private void createRouteForCommand(Class<?> commandClazz) {

    final GsonDataFormat df = new GsonDataFormat(gson, commandClazz);

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
		final SnapshotReader.Snapshot<A> snapshot = snapshotReader.getSnapshot(targetId);
		final Optional<UnitOfWork> unitOfWork;
		CommandExecution result;
		try {
			unitOfWork = handler.handleCommand(_command,
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
			if (uow.isPresent()) {
				writeModelRepo.append(uow.get(), _command, command1 -> commandId);
				e.getOut().setBody(uow.get(), UnitOfWork.class);
				e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
			} else {
				e.getOut().setBody(null);
				e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
			}
		})
		.ERROR(exception -> () -> {
			e.getOut().setBody(Arrays.asList(exception.getMessage()), List.class);
			e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
		});
		r.run();
		}
	}
}
