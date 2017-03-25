package myeslib3.example1.core.aggregates.customer


import com.spencerwi.either.Result
import myeslib3.core.AggregateRoot
import myeslib3.core.UnitOfWork
import myeslib3.core.Version
import myeslib3.core.command.CommandHandlerFn
import myeslib3.core.command.WriteModelStateTracker
import myeslib3.core.command.WriteModelStateTransitionFn
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

// a helper service

open class SupplierHelperService {
    open fun now() = LocalDateTime.now()
    open fun uuId() = UUID.randomUUID()
}

// aggregate root

data class Customer(val customerId: String? = null,
                    val name: String? = null,
                    val active: Boolean = false,
                    val activatedSince: LocalDateTime? = null,
                    val deactivatedSince: LocalDateTime? = null,
                    val reason: String? = null) : AggregateRoot {

    @Inject lateinit var genValService: SupplierHelperService

    // behaviour

    fun create(customerId: String, name: String): List<CustomerEvent> {
        // check if new then
        require(this.customerId == null, { "customer already exists! customerId should be null but is $this.customerId" })
        return listOf(CustomerCreated(customerId, name))
    }

    fun activate(reason: String): List<CustomerEvent> {
        return listOf(CustomerActivated(reason, genValService.now()),
                DeactivatedCmdScheduled(
                        DeactivateCustomerCmd("just because I want automatic deactivation 1 day after activation"),
                        genValService.now().plusDays(1)))
    }

    fun deactivate(reason: String): List<CustomerEvent> {
        return listOf(CustomerDeactivated(reason, genValService.now()))
    }
}

// events routing and execution function

val WRITE_MODEL_STATE_TRANSITION_FN: WriteModelStateTransitionFn<Customer> = WriteModelStateTransitionFn { event, state ->
    when (event) {
        is CustomerCreated -> state.copy(customerId = event.customerId, name = event.name)
        is CustomerActivated -> state.copy(active = true, activatedSince = event.date, reason = event.reason)
        is CustomerDeactivated -> state.copy(active = false, deactivatedSince = event.date, reason = event.reason)
        is DeactivatedCmdScheduled -> state
        else -> state
    }
}

// commands routing and execution function

val COMMAND_HANDLER_FN: CommandHandlerFn<Customer, CustomerCommand> = CommandHandlerFn {
    commandId, command, targetId, targetInstance, targetVersion, stateTransitionFn, injectionFn ->

    // TODO consider fold operation instead https://gist.github.com/cy6erGn0m/6960104
    val trackerWriteModel: WriteModelStateTracker<Customer> =
            WriteModelStateTracker(targetInstance, stateTransitionFn, injectionFn)

    Result.attempt {
        when (command) {
            is CreateCustomerCmd -> {
                require(targetVersion == Version.create(0), { "before create the instance must be version= 0" })
                UnitOfWork.create(targetId, commandId, targetVersion.nextVersion(),
                        targetInstance.create(targetId, command.name))
            }
            is ActivateCustomerCmd ->
                UnitOfWork.create(targetId, commandId, targetVersion.nextVersion(),
                        targetInstance.activate(command.reason))
            is DeactivateCustomerCmd ->
                UnitOfWork.create(targetId, commandId, targetVersion.nextVersion(),
                        targetInstance.deactivate(command.reason))
            is CreateActivatedCustomerCmd -> {
                val events = trackerWriteModel
                        .applyEvents(targetInstance.create(targetId, command.name))
                        .applyEvents(trackerWriteModel.currentState().activate(command.reason))
                        .collectedEvents()
                UnitOfWork.create(targetId, commandId, targetVersion.nextVersion(), events)
            }
            else -> {
                throw IllegalArgumentException("invalid command")
            }
        }
    }
}


