package org.smartsoft.konfigurator

import org.smartsoft.konfigurator.data.CarSpace
import kotlin.test.Test
import kotlin.test.assertEquals


class CarTest {

    @Test
    fun test1() {
        CarSpace().run {
            val rs1 = mkRuleSet1()
            val rs2 = rs1.assign(L4, Base, T6AT)
            val rs3 = rs1.assign(T2514)
            assertEquals(rs2.effectiveConstraint, rs3.effectiveConstraint)
            assertEquals(rs2.lits, rs3.lits)
        }


    }


}

