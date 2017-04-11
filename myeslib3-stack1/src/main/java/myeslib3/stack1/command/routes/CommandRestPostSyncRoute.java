package myeslib3.stack1.command.routes;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.UnitOfWork;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;

import java.util.List;

import static myeslib3.stack1.stack1infra.utils.StringHelper.*;

@AllArgsConstructor
public class CommandRestPostSyncRoute<A extends AggregateRoot> extends RouteBuilder {

	static final String AGGREGATE_ROOT_ID = "aggregate_root_id";
  static final String COMMAND_ID = "command_id";
  static final String APPLICATION_JSON = "application/json";

	@NonNull final Class<A> aggregateRootClass;
	@NonNull final List<Class<?>> commandsClasses;

	@Override
  public void configure() throws Exception {

    restConfiguration().component("undertow").bindingMode(RestBindingMode.auto)
            .dataFormatProperty("prettyPrint", "true")
            .contextPath("/").port(8080)
            .apiContextPath("/api-doc")
            .apiProperty("api.title", "Customer API").apiProperty("api.version", "1.0.0")
            .enableCORS(true);

    commandsClasses.forEach(this::createRouteForCommand);

  }

  private void createRouteForCommand(Class<?> commandClazz) {

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

  }

}
