package myeslib3.commands_flow

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import myeslib3.core.data.Command
import myeslib3.core.data.Event
import myeslib3.core.data.Version
import myeslib3.dependencyInjectionFn
import myeslib3.examples.example1.core.aggregates.customer.*
import myeslib3.helpers.RuntimeTypeAdapterFactory
import myeslib3.persistence.SnapshotReader
import myeslib3.persistence.SnapshotReader.Snapshot
import net.dongliu.gson.GsonJava8TypeAdapterFactory
import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import java.util.*
import java.util.function.Supplier

class PostCommandRouteIT {

    var context: CamelContext = DefaultCamelContext()

    @BeforeEach
    @Throws(Exception::class)
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = DefaultCamelContext()
        val commandList = Arrays.asList<Class<*>>(CreateCustomerCmd::class.java, ActivateCustomerCmd::class.java,
                CreateActivatedCustomerCmd::class.java, DeactivateCustomerCmd::class.java)
        val route = PostCommandRoute<Customer, CustomerCommand>(Customer::class.java, commandList,
                commandHandlerFn, stateTransitionFn, dependencyInjectionFn, CaffeineSnapShotReader(), gson())
        context.addRoutes(route)
        context.start()
    }

    @AfterEach
    @Throws(Exception::class)
    fun afterRun() {
        context.stop()
    }

    @Test
    fun aTest() {
        assertThat(1).isEqualTo(1)
        Thread.sleep(4000000)
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

        gsonBuilder.registerTypeAdapterFactory(GsonJava8TypeAdapterFactory()).create()

        val gson = gsonBuilder.create()

        return gson
    }
}

class CaffeineSnapShotReader : SnapshotReader<String, Customer> {

    val supplier: Supplier<Customer> = Supplier { Customer() }

    override fun getSnapshot(id: String): Snapshot<Customer> {
        return Snapshot(dependencyInjectionFn.inject(supplier.get()), Version(0))
    }

}