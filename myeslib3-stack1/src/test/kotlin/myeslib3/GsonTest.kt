package myeslib3

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import myeslib3.core.data.Command
import myeslib3.core.data.Event
import myeslib3.core.data.UnitOfWork
import myeslib3.core.data.Version
import myeslib3.core.functions.DependencyInjectionFn
import myeslib3.example1.core.aggregates.customer.*
import myeslib3.stack1.stack1infra.gson.RuntimeTypeAdapterFactory
import net.dongliu.gson.GsonJava8TypeAdapterFactory
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*

val customerId = "customer#1"
val commandId = "command#1"

// https://dzone.com/articles/deserialization-1

fun main(args: Array<String>) {

    val gson = gson()

    val uow1 = uow1().get()
    val uowAsJson1 = gson.toJson(uow1)
    println(uowAsJson1)
    val fromJsonUow1 = gson.fromJson(uowAsJson1, UnitOfWork::class.java)
    assertEquals(fromJsonUow1, uow1)

    val uow2 = uow2().get()
    val uowAsJson2 = gson.toJson(uow2)
    println(uowAsJson2)
    val fromJsonUow2 = gson.fromJson(uowAsJson2, UnitOfWork::class.java)
    assertEquals(fromJsonUow2, uow2)

}

val dependencyInjectionFn: DependencyInjectionFn<Customer> = DependencyInjectionFn { customer ->
    customer.genValService = SupplierHelperService()
    customer
}

fun uow1(): Optional<UnitOfWork> {
    val cmd: CreateCustomerCmd = CreateCustomerCmd("customer1")
    val customer = dependencyInjectionFn.inject(Customer())
    val version = Version.create(0)
    return COMMAND_HANDLER_FN.handleCommand(cmd, customerId, customer, version, STATE_TRANSITION_FN, dependencyInjectionFn)
}

fun uow2(): Optional<UnitOfWork> {
    val cmd = CreateActivatedCustomerCmd("customer1", "because I want it")
    val customer = dependencyInjectionFn.inject(Customer())
    val version = Version.create(0)
    return COMMAND_HANDLER_FN.handleCommand(cmd, customerId, customer, version, STATE_TRANSITION_FN, dependencyInjectionFn)
}

fun gson(): Gson {

    val rtaCommand: RuntimeTypeAdapterFactory<Command> = RuntimeTypeAdapterFactory.of(Command::class.java)
            .registerSubtype(CreateCustomerCmd::class.java)
            .registerSubtype(ActivateCustomerCmd::class.java)
            .registerSubtype(DeactivateCustomerCmd::class.java)
            .registerSubtype(CreateActivatedCustomerCmd::class.java)

    val rtaEvents: RuntimeTypeAdapterFactory<Event> = RuntimeTypeAdapterFactory.of(Event::class.java)
            .registerSubtype(CustomerCreated::class.java)
            .registerSubtype(CustomerActivated::class.java)
            .registerSubtype(CustomerDeactivated::class.java)
            .registerSubtype(DeactivatedCmdScheduled::class.java)

    val gsonBuilder = GsonBuilder()

    gsonBuilder.setPrettyPrinting()
    gsonBuilder.registerTypeAdapterFactory(GsonJava8TypeAdapterFactory())
    gsonBuilder.registerTypeAdapterFactory(rtaCommand)
    gsonBuilder.registerTypeAdapterFactory(rtaEvents)

    val gson = gsonBuilder.create()

    return gson
}