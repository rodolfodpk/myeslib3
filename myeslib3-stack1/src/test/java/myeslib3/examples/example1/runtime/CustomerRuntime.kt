package myeslib3.examples.example1.runtime

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Provides
import myeslib3.core.functions.DependencyInjectionFn
import myeslib3.examples.example1.core.aggregates.customer.Customer
import myeslib3.examples.example1.core.aggregates.customer.SupplierHelperService

// dependencies

class CustomerModule : AbstractModule() {

    override fun configure() {
        bind(SupplierHelperService::class.java)
    }

    @Provides
    fun injectorFn(injector: Injector): DependencyInjectionFn<Customer> =
            DependencyInjectionFn { c -> injector.injectMembers(c); c }


}

