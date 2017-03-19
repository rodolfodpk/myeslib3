package myeslib3.stack1

import com.google.gson.Gson
import com.google.inject.Guice
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import myeslib3.core.StateTransitionsTracker
import myeslib3.core.data.Version
import myeslib3.dependencyInjectionFn
import myeslib3.examples.example1.core.aggregates.customer.*
import myeslib3.examples.example1.runtime.CustomerModule
import myeslib3.stack.SnapshotReader
import myeslib3.stack.WriteModelRepository
import myeslib3.stack1.routes.CommandPostSyncRoute
import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.spi.IdempotentRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*
import java.util.function.Supplier

class PostCommandRouteTest {

    val injector = Guice.createInjector(CustomerModule())
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
            on { getSnapshot(argThat { true /* whatever */ }, any()) } doReturn
                    SnapshotReader.Snapshot(dependencyInjectionFn.inject(supplier.get()), Version(0))
        }

        val writeModelDao = mock<WriteModelRepository> {}

        val idempotentRepoMock = mock<IdempotentRepository<String>> {}

        val route = CommandPostSyncRoute<Customer, CustomerCommand>(Customer::class.java, commandList,
                commandHandlerFn, stateTransitionFn, supplier,
                dependencyInjectionFn, readerMock,
                writeModelDao, injector.getInstance(Gson::class.java), idempotentRepoMock)

        context.addRoutes(route)
        context.start()
    }

    @AfterEach
    @Throws(Exception::class)
    fun afterRun() {
        context.stop()
    }

    @Test
    @Disabled
    fun aTest() {
        assertThat(1).isEqualTo(1)
        Thread.sleep(4000000)
    }

}
