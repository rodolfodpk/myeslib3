package myeslib3.stack1.query.routes;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.impossibl.postgres.jdbc.PGDataSource;
import lombok.val;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.example1.Example1Module;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.Stack1Config;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.stack1infra.BoundedContextConfig;
import myeslib3.stack1.stack1infra.DatabaseConfig;
import myeslib3.stack1.stack1infra.DatabaseModule;
import org.aeonbits.owner.ConfigCache;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static myeslib3.stack1.stack1infra.utils.ConfigHelper.overrideConfigPropsWithSystemVars;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class EventsPgEventRouteTest extends CamelTestSupport {

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

  @Inject
  BoundedContextConfig bcConfig;

  @Inject
  PGDataSource ds;

  @Mock
  WriteModelRepository repo;

  @BeforeEach
  public void init() throws Exception {
    injector.injectMembers(this);
    MockitoAnnotations.initMocks(this);
    val route = new EventsPollingRoute(eventsChannelId, repo, bcConfig);
  }

  @Test
  @Disabled // TODO
  public void producing_uow_should_work() throws InterruptedException {

    val cmd1 = new CreateCustomerCmd(UUID.randomUUID(), new CustomerId("c1"), "customer1");
    val event1 = new CustomerCreated(cmd1.getTargetId(), cmd1.getName());
    val uow1 = UnitOfWork.of(cmd1, new Version(1), Arrays.asList(event1));

    when(repo.get(eq(uow1.getUnitOfWorkId()))).thenReturn(Optional.of(uow1));

    template.sendBody(uow1.getUnitOfWorkId().toString());

    Thread.sleep(10000);

    System.out.println(resultEndpoint.getExchanges().get(0).getIn().getBody());

//    resultEndpoint.expectedBodiesReceived(uow1);

    resultEndpoint.assertIsSatisfied();

  }

  @Override
  protected JndiRegistry createRegistry() throws Exception {
    final DatabaseConfig dc = injector.getInstance(DatabaseConfig.class);
    final PGDataSource ds = injector.getInstance(PGDataSource.class);
    JndiRegistry jndi = super.createRegistry();
    jndi.bind("ds", ds);
    return jndi;
  }

  @Override
  protected RouteBuilder createRouteBuilder() {
    return new RouteBuilder() {
      public void configure() {

        final DatabaseConfig dc = injector.getInstance(DatabaseConfig.class);

        from("direct:start")
          .log("received ${body}")
//          .toF("pgevent://%s:%s/%s/%s?user=%s&pass=%s",
//                  dc.db_host(), dc.db_port(), dc.db_name(), eventsChannelId, dc.db_user(), dc.db_password());
                .toF("pgevent:%s/%s/%s","localhost", dc.db_name(), eventsChannelId);

        fromF("seda:%s-events?multipleConsumers=true", eventsChannelId)
          .log("** received ${body}")
          .to("mock:result");

      }
    };

  }

}