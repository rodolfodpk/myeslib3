package myeslib3.stack1

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import myeslib3.core.StateTransitionsTracker
import myeslib3.core.data.Command
import myeslib3.core.data.Event
import myeslib3.core.data.Version
import myeslib3.dependencyInjectionFn
import myeslib3.examples.example1.core.aggregates.customer.*
import myeslib3.stack.SnapshotReader
import myeslib3.stack.WriteModelDao
import myeslib3.stack1.infra.gson.RuntimeTypeAdapterFactory
import myeslib3.stack1.routes.CommandPostSyncRoute
import net.dongliu.gson.GsonJava8TypeAdapterFactory
import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.spi.IdempotentRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.function.Supplier

class PostCommandRouteIT {

    var context: CamelContext = DefaultCamelContext()

    val customerId = "customer#1"
    val commandId = "command#1"

    @BeforeEach
    @Throws(Exception::class)
    fun setup() {
        //MockitoAnnotations.initMocks(this)
        context = DefaultCamelContext()

        val commandList = Arrays.asList<Class<*>>(CreateCustomerCmd::class.java, ActivateCustomerCmd::class.java,
                CreateActivatedCustomerCmd::class.java, DeactivateCustomerCmd::class.java)

        val supplier: Supplier<Customer> = Supplier { Customer() }
        val tracker = StateTransitionsTracker<Customer>(supplier.get(),
                stateTransitionFn, dependencyInjectionFn)

        val readerMock = mock<SnapshotReader<Customer>> {
            on { getSnapshot(argThat { true /* whatever */ }, tracker) } doReturn SnapshotReader.Snapshot(dependencyInjectionFn.inject(supplier.get()), Version(0))
        }
        val writeModelDao = mock<WriteModelDao> {}
        val idempotentRepoMock = mock<IdempotentRepository<String>> {}

        val route = CommandPostSyncRoute<Customer, CustomerCommand>(Customer::class.java, commandList,
                commandHandlerFn, stateTransitionFn, supplier,
                dependencyInjectionFn, readerMock,
                writeModelDao, gson(), idempotentRepoMock)

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
