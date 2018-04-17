package org.smartsoft.konfigurator

import org.junit.Before
import org.smartsoft.konfigurator.data.CarExample
import kotlin.test.Test
import kotlin.test.assertEquals


class CarExampleTest {

    lateinit var vars: CarExample
    lateinit var csp: Csp

    @Before
    fun setup() {
        vars = CarExample()
        csp = vars.buildCsp()
    }

    @Test
    fun test1() {
        with(vars) {
            val csp1 = csp.assign(L4, Base, T6AT)
            val csp2 = csp.assign(T2514)
            assertEquals(csp1.effectiveConstraint, csp2.effectiveConstraint)
            assertEquals(csp1.lits, csp2.lits)
        }
    }


}

