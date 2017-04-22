package myeslib3.stack1.query.routes;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.val;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.example1.Example1Module;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.Stack1Config;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.stack1infra.DatabaseConfig;
import myeslib3.stack1.stack1infra.DatabaseModule;
import org.aeonbits.owner.ConfigCache;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static myeslib3.stack1.stack1infra.utils.ConfigHelper.overrideConfigPropsWithSystemVars;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class EventsFromTopicRouteTest extends CamelTestSupport {

  final static String eventsChannelId = "channelExample1";

  static final Injector injector = Guice.createInjector(new Example1Module(), new DatabaseModule(), new AbstractModule() {
    @Override
    protected void configure() {
      Stack1Config config = ConfigCache.getOrCreate(Stack1Config.class, System.getProperties(), System.getenv());
      bind(DatabaseConfig.class).toInstance(config);
      overrideConfigPropsWithSystemVars(binder(), config);
    }

//    @Provides
//    @Singleton
//    public WriteModelRepository<CustomerId> repo(Gson gson, DBI dbi) {
//      return new Stack1WriteModelRepository<>(eventsChannelId, "Customer", gson, dbi);
//    }

  });

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Inject
  DatabaseConfig dc;

  @Mock
  WriteModelRepository<CustomerId> repo;

  @Before
  public void init() throws Exception {
    injector.injectMembers(this);
    MockitoAnnotations.initMocks(this);
    val route = new EventsFromTopicRoute(eventsChannelId, repo, dc);
    context.addRoutes(route);
  }

  @Test
  public void producing_uow_should_work() throws InterruptedException {

    val cmd1 = new CreateCustomerCmd(UUID.randomUUID(), new CustomerId("c1"), "customer1");
    val event1 = new CustomerCreated(cmd1.getTargetId(), cmd1.getName());
    val uow1 = UnitOfWork.create(cmd1, new Version(1), Arrays.asList(event1));

    when(repo.get(eq(uow1.getUnitOfWorkId()))).thenReturn(Optional.of(uow1));

    resultEndpoint.expectedBodiesReceived(uow1);

    resultEndpoint.assertIsSatisfied();

  }

  @Override
  protected RouteBuilder createRouteBuilder() {
    return new RouteBuilder() {
      public void configure() {

        final DatabaseConfig dc = injector.getInstance(DatabaseConfig.class);

        from("direct:start")
            .toF("pgevent://%s:%s/%s/%s?user=%s&pass=%s",
                  dc.db_host(), dc.db_port(), dc.db_name(), eventsChannelId, dc.db_user(), dc.db_password());

        fromF("seda:%s-events?multipleConsumers=true", eventsChannelId)
          .log("** received ${body}")
          .to("mock:result");

      }
    };

  }

}