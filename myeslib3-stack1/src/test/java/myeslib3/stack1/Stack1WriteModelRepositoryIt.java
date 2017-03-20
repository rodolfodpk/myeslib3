package myeslib3.stack1;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.examples.example1.core.aggregates.customer.CreateCustomerCmd;
import myeslib3.examples.example1.core.aggregates.customer.CustomerCreated;
import myeslib3.examples.example1.runtime.CustomerModule;
import myeslib3.stack1.infra.DatabaseModule;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

import javax.inject.Inject;
import java.util.Arrays;

public class Stack1WriteModelRepositoryIt {

	final static Injector injector = Guice.createInjector(new CustomerModule(),
					new Stack1Module(),
					new DatabaseModule());

	@Inject
	Gson gson;
	@Inject
	DBI dbi;

	Stack1WriteModelRepository repo ;

	@Before
	public void setup() {
			injector.injectMembers(this);
			repo = new Stack1WriteModelRepository("customer", gson, dbi);
	}

	@Test
	public void append() {

		String id = "c1";
		CreateCustomerCmd command = new CreateCustomerCmd("c1");
		CustomerCreated event = new CustomerCreated(id, command.getName());
		UnitOfWork uow1 = UnitOfWork.create(id, command.getName(), command, Version.create(1),  Arrays.asList(event));

		repo.append(uow1);

	}
}
