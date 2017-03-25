package myeslib3.example1.core.projections

import myeslib3.core.query.ReadModelStateTransitionFn
import myeslib3.example1.core.aggregates.customer.CustomerActivated
import myeslib3.example1.core.aggregates.customer.CustomerCreated

// TOTO considerar customizar Speedment pra gerar Kotlin data classes
data class CustomerSummary(val id: String, val activationsCount: Int)

// events routing and execution function

val customerSummaryStateTransitionFn: ReadModelStateTransitionFn<CustomerSummary> = ReadModelStateTransitionFn {
    event, state ->
    when (event) {
        is CustomerCreated -> state.copy(id = event.customerId)
        is CustomerActivated -> state.copy(activationsCount = state.activationsCount+1)
        else -> state
    }
}
