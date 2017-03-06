package myeslib3.examples.example1.core.aggregates.customer

import myeslib3.core.data.Command
import myeslib3.core.data.CommandScheduling
import myeslib3.core.data.Event
import java.time.LocalDateTime

// customer commands

sealed class CustomerCommand : Command

data class CreateCustomerCmd(val name: String) : CustomerCommand()

data class ActivateCustomerCmd(val reason: String) : CustomerCommand()

data class CreateActivatedCustomerCmd(val name: String, val reason: String) : CustomerCommand()

data class DeactivateCustomerCmd(val reason: String) : CustomerCommand()

// customer events

sealed class CustomerEvent : Event

data class CustomerCreated(val customerId: String, val name: String) : CustomerEvent()

data class CustomerActivated(val reason: String, val date: LocalDateTime) : CustomerEvent()

data class DeactivatedCmdScheduled(val scheduledCommand: DeactivateCustomerCmd,
                                   val scheduledAt: LocalDateTime) : CustomerEvent(), CommandScheduling {
    override fun scheduledCommand(): Command {
        return scheduledCommand
    }
    override fun scheduledAt(): LocalDateTime {
        return scheduledAt
    }
}

data class CustomerDeactivated(val reason: String, val date: LocalDateTime) : CustomerEvent()

// views TODO

