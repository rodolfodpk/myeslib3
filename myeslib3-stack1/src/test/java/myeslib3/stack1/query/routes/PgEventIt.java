package myeslib3.stack1.query.routes;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.impossibl.postgres.jdbc.PGDataSource;
import myeslib3.example1.aggregates.customer.CustomerModule;
import myeslib3.stack1.Stack1Module;
import myeslib3.stack1.stack1infra.DatabaseConfig;
import myeslib3.stack1.stack1infra.DatabaseModule;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;

public class PgEventIt {

	final static Injector injector = Guice.createInjector(new CustomerModule(),
																												new Stack1Module(),
																											  new DatabaseModule());

	final static PGDataSource ds = injector.getInstance(PGDataSource.class);
	final static DatabaseConfig dc = injector.getInstance(DatabaseConfig.class);

	static Main main;

	public static void main(String a[]) throws Exception {

		main = new Main();
	//	main.bind(dc.db_name(), ds);
		main.addRouteBuilder(buildConsumer());
		main.addRouteBuilder(buildProducer());

		main.run();

	}

	static RouteBuilder buildConsumer() {
		RouteBuilder builder = new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				fromF("pgevent:%s/%s/%s", "localhost", dc.db_name(), "testchannel")
//								.log("${body}")
								.to("log:org.apache.camel.pgevent.PgEventConsumer?level=INFO");
			}
		};

		return builder;
	}

	static RouteBuilder buildProducer() {
		RouteBuilder builder = new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				from("timer://test?fixedRate=true&period=5000")
								.setBody(header(Exchange.TIMER_FIRED_TIME))
								.toF("pgevent:%s/%s/%s","localhost", dc.db_name(), "testchannel")
//								.log("${body}")
								.to("log:org.apache.camel.pgevent.PgProducer?level=INFO");
			}
		};

		return builder;
	}


}