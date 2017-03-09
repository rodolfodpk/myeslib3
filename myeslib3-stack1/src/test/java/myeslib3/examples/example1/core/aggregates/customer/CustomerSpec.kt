package myeslib3.examples.example1.core.aggregates.customer

import com.nhaarman.mockito_kotlin.*
import io.kotlintest.specs.BehaviorSpec
import myeslib3.core.data.UnitOfWork
import myeslib3.core.data.Version
import myeslib3.core.functions.DependencyInjectionFn
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class CustomerSpec : BehaviorSpec() {

    val dependencyInjectionFn : DependencyInjectionFn<Customer> = DependencyInjectionFn { customer ->
        customer.genValService = SupplierHelperService()
        customer
    }

    val customerId = "customer#1"
    val commandId = "command#1"

    init {
        Given("an empty Customer with version 0") {
            val customer = Customer()
            val version = Version.create(0)
            When("a createCommand is issued") {
                val cmd = CreateCustomerCmd(name = "customer 1")
                val operation = commandHandlerFn.handle(commandId, cmd,
                        customerId, customer, version,
                        stateTransitionFn, dependencyInjectionFn)
                val uow: UnitOfWork = operation.result
                Then("a proper UnitOfWork is generated") {
                    assertThat(uow.commandId).isEqualTo(commandId)
                    assertThat(uow.aggregateRootId).isEqualTo(customerId)
                    assertThat(uow.command).isEqualTo(cmd)
                    assertThat(uow.version).isEqualTo(Version.create(1))
                    assertThat(uow.events.first()).isEqualTo(CustomerCreated(customerId, cmd.name))
                }
            }
        }
        Given("a non active Customer with version 1") {
            // just a service mock
            val activatedOn = LocalDateTime.now()
            val uuid = UUID.randomUUID()
            val serviceMock = mock<SupplierHelperService> {
                on { now() } doReturn activatedOn
                on { uuId() } doReturn uuid
            }
            val customer = Customer(customerId = customerId, name = "customer1", active = false,
                    activatedSince = null, deactivatedSince = null)
            customer.genValService = serviceMock
            val version = Version.create(1)
            When("an activateCommand is issued") {
                val cmd = ActivateCustomerCmd("because I want it")
                val operation = commandHandlerFn.handle(commandId, cmd,
                        customerId, customer, version,
                        stateTransitionFn, dependencyInjectionFn)
                val uow: UnitOfWork = operation.result
                Then("a proper UnitOfWork is generated") {
                    val expectedCmd =
                            DeactivateCustomerCmd("just because I want automatic deactivation 1 day after activation")
                    assertThat(uow.commandId).isEqualTo(commandId)
                    assertThat(uow.aggregateRootId).isEqualTo(customerId)
                    assertEquals(uow.command, cmd)
                    assertEquals(uow.version, Version.create(2))
                    assertEquals(uow.events.first(), CustomerActivated(cmd.reason, activatedOn))
                    assertThat(uow.events.last()).isEqualTo(DeactivatedCmdScheduled(expectedCmd, activatedOn.plusDays(1)))
                }
                Then("now() is called on  serviceMock") {
                    verify(serviceMock, times(2)).now() // one for activate other for deactivate schedule date
                }
            }
        }
        Given("a non active Customer with version 1") {
            // just a service mock
            val activatedOn = LocalDateTime.now()
            val serviceMock = mock<SupplierHelperService> {
                on { now() } doReturn activatedOn
            }
            val customer = Customer(customerId = customerId, name = "customer1", active = false,
                    activatedSince = null, deactivatedSince = null)
            customer.genValService = serviceMock
            val version = Version.create(1)
            When("a createCommand with same customerId is issued") {
                val cmd = CreateCustomerCmd(name = "customer1")
                val operation = commandHandlerFn.handle(commandId, cmd,
                        customerId, customer, version,
                        stateTransitionFn, dependencyInjectionFn)
                Then("result must be an error with an IllegalArgumentException") {
                    val exception = operation.exception
                    assertEquals(exception?.localizedMessage, "before create the instance must be version= 0")
                    assertEquals(exception!!.javaClass.name, IllegalArgumentException::class.java.name)
                }
                Then("nothing is called on serviceMock") {
                    verifyNoMoreInteractions(serviceMock)
                }
            }
        }
        Given("an empty Customer with version 1") {
            val customer = Customer()
            val version = Version.create(1)
            val cmd = CreateCustomerCmd(name = "customer1")
            When("a createCommand is issued") {
                val operation = commandHandlerFn.handle(commandId, cmd,
                        customerId, customer, version,
                        stateTransitionFn, dependencyInjectionFn)
                Then("result must be an error with an IllegalArgumentException") {
                    val exception = operation.exception
                    assertEquals(exception?.localizedMessage, "before create the instance must be version= 0")
                    assertEquals(exception!!.javaClass.name, IllegalArgumentException::class.java.name)
                }
            }
        }

    }
}
