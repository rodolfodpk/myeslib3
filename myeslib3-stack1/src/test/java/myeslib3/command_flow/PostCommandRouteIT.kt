package myeslib3.command_flow

import myeslib3.core.data.Version
import myeslib3.dependencyInjectionFn
import myeslib3.examples.example1.core.aggregates.customer.*
import myeslib3.persistence.SnapshotReader
import myeslib3.persistence.SnapshotReader.Snapshot
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
        val route = PostCommandRoute<Customer, CustomerCommand>(Customer::class.java, commandList, commandHandlerFn, stateTransitionFn, dependencyInjectionFn, CaffeineSnapShotReader())
        context.addRoutes(route)
        context.start()
    }

    @AfterEach
    @Throws(Exception::class)
    fun afterRun() {
        context.stop()
    }

//    @Test
    fun aTest() {
        assertThat(1).isEqualTo(1)
        Thread.sleep(4000000)
    }

}

class CaffeineSnapShotReader : SnapshotReader<String, Customer> {

    val supplier: Supplier<Customer> = Supplier { Customer() }

    override fun getSnapshot(id: String): Snapshot<Customer> {
        return Snapshot(dependencyInjectionFn.inject(supplier.get()), Version(0))
    }

}