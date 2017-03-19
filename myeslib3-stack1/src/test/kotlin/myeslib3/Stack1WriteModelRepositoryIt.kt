package myeslib3

import com.google.gson.Gson
import com.google.inject.Guice
import myeslib3.core.data.UnitOfWork
import myeslib3.core.data.Version
import myeslib3.examples.example1.core.aggregates.customer.*
import myeslib3.examples.example1.runtime.CustomerModule
import myeslib3.stack1.Stack1Module
import myeslib3.stack1.Stack1WriteModelRepository
import myeslib3.stack1.infra.CamelModule
import myeslib3.stack1.infra.DatabaseModule
import org.junit.jupiter.api.Test
import org.skife.jdbi.v2.DBI

class Stack1WriteModelRepositoryIt {

    @Test
    fun aTest() {

        val injector = Guice.createInjector(Stack1Module(), CustomerModule(), CamelModule(), DatabaseModule())

        val dbi: DBI = injector.getInstance(DBI::class.java)
        val gson = injector.getInstance(Gson::class.java)

        val repository = Stack1WriteModelRepository("customer", gson, dbi)

        repository.append(uow2())

    }

    fun uow1(): UnitOfWork {
        val cmd: CreateCustomerCmd = CreateCustomerCmd("customer1")
        val customer = dependencyInjectionFn.inject(Customer())
        val version = Version.create(0)
        return commandHandlerFn.handle(commandId, cmd, customerId, customer, version, stateTransitionFn, dependencyInjectionFn).result
    }

    fun uow2(): UnitOfWork {
        val cmd = CreateActivatedCustomerCmd("customer3", "because I also want it")
        val customer = dependencyInjectionFn.inject(Customer())
        val version = Version.create(0)
        return commandHandlerFn.handle(commandId, cmd, "customer#3", customer, version, stateTransitionFn, dependencyInjectionFn).result
    }


}
