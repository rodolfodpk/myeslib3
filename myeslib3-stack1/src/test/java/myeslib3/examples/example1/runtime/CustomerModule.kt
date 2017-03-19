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
import myeslib3.core.functions.StateTransitionFn
import myeslib3.examples.example1.core.aggregates.customer.*
import myeslib3.stack1.infra.gson.RuntimeTypeAdapterFactory
import net.dongliu.gson.GsonJava8TypeAdapterFactory

// dependencies

class CustomerModule : AbstractModule() {

    override fun configure() {
        bind(SupplierHelperService::class.java)
    }

    @Provides
    @Singleton
    fun injectorFn(service: SupplierHelperService): DependencyInjectionFn<Customer> =
            DependencyInjectionFn { c -> c.genValService = service; c }

    @Provides
    @Singleton
    fun stateTransitionFn(): StateTransitionFn<Customer> {
        return stateTransitionFn
    }

    @Provides
    @Singleton
    fun commandHandlerFn(): CommandHandlerFn<Customer, CustomerCommand> {
        return commandHandlerFn
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

