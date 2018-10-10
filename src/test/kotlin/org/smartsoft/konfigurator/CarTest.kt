package org.smartsoft.konfigurator

import org.smartsoft.konfigurator.data.Car
import kotlin.test.Test
import kotlin.test.assertEquals


class CarTest {

    @Test
    fun test1() {
        Car().run {
            val cs1 = mkConstraintSet1()
            val cs2 = cs1.assign(L4, Base, T6AT)
            val cs3 = cs1.assign(T2514)
            assertEquals(cs2.effectiveConstraint, cs3.effectiveConstraint)
            assertEquals(cs2.lits, cs3.lits)
        }


    }


}

