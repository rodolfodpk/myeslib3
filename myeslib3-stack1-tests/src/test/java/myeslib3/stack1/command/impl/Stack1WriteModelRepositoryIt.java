package myeslib3.stack1.command.impl;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.example1.Example1Module;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.stack1infra.DatabaseModule;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.TransactionCallback;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class Stack1WriteModelRepositoryIt {

	final static Injector injector = Guice.createInjector(
					new DatabaseModule(),
					new Example1Module());

	@Inject
	Gson gson;
	@Inject
	DBI dbi;

	Stack1WriteModelRepository repo ;

	@Before
	public void setup() {
		injector.injectMembers(this);
		repo = new Stack1WriteModelRepository("customer", gson, dbi);
		dbi.inTransaction((TransactionCallback<Void>) (handle, transactionStatus) -> {
			handle.execute("delete from idempotency");
      handle.execute("delete from aggregate_roots");
      handle.execute("delete from units_of_work");
      return null;
    });
	}

	@Test
	public void append() {

		final CustomerId id = new CustomerId("customer#1");
		final CreateCustomerCmd command = new CreateCustomerCmd(UUID.randomUUID(), id, "customer1");
		final CustomerCreated event = new CustomerCreated(id, command.getName());
		final UnitOfWork uow1 = UnitOfWork.of(command, Version.create(1), Arrays.asList(event));

		repo.append(uow1);

		assertThat(repo.get(uow1.getUnitOfWorkId()).get()).isEqualTo(uow1);

	}
}
