package myeslib3.examples.example1.runtime

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import myeslib3.core.data.Command
import myeslib3.core.data.Event
import myeslib3.core.functions.CommandHandlerFn
import myeslib3.core.functions.DependencyInjectionFn
import myeslib3.core.functions.SagaEventMonitoringFn
import myeslib3.core.functions.StateTransitionFn
import myeslib3.example1.core.aggregates.customer.*
import myeslib3.stack1.stack1infra.gson.RuntimeTypeAdapterFactory
import net.dongliu.gson.GsonJava8TypeAdapterFactory
import java.util.function.Supplier

// dependencies

class CustomerModule : AbstractModule() {

    override fun configure() {
        bind(SupplierHelperService::class.java)
    }

    @Provides
    @Singleton
    fun supplier(): Supplier<Customer> {
        return Supplier { Customer() }
    }

    @Provides
    @Singleton
    fun injectorFn(service: SupplierHelperService): DependencyInjectionFn<Customer> =
            DependencyInjectionFn { c -> c.genValService = service; c }

    @Provides
    @Singleton
    fun stateTransitionFn(): StateTransitionFn<Customer> {
        return STATE_TRANSITION_FN
    }

    @Provides
    @Singleton
    fun commandHandlerFn(): CommandHandlerFn<Customer, CustomerCommand> {
        return myeslib3.example1.core.aggregates.customer.COMMAND_HANDLER_FN
    }

    @Provides
    @Singleton
    fun eventMonitoringFn(): SagaEventMonitoringFn {
        return myeslib3.example1.core.aggregates.customer.EVENT_MONITORING_FN
    }

    @Provides
    @Singleton
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

}

