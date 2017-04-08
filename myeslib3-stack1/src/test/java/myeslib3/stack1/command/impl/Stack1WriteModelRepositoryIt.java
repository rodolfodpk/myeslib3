package myeslib3.stack1.command.impl;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.example1.core.aggregates.customer.CreateCustomerCmd;
import myeslib3.example1.core.aggregates.customer.CustomerCreated;
import myeslib3.examples.example1.runtime.CustomerModule;
import myeslib3.stack1.Stack1Module;
import myeslib3.stack1.stack1infra.DatabaseModule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

import javax.inject.Inject;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

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
			repo = new Stack1WriteModelRepository("example1_uow_channel", "customer", gson, dbi);
	}

	@Test @Ignore
	public void append() {

		String id = "c1";
		String cmdId = "cmd1";
		CreateCustomerCmd command = new CreateCustomerCmd("c1");
		CustomerCreated event = new CustomerCreated(id, command.getName());
		UnitOfWork uow1 = UnitOfWork.create(id, Version.create(1), Arrays.asList(event));

		repo.append(uow1, command, command1 -> cmdId);

		assertThat(repo.get(uow1.getUnitOfWorkId())).isEqualTo(uow1);

	}
}
