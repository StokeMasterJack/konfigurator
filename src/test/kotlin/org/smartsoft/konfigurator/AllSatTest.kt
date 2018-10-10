package org.smartsoft.konfigurator

import org.smartsoft.konfigurator.data.Car
import org.smartsoft.konfigurator.data.Simple
import kotlin.test.Test
import kotlin.test.assertEquals


class AllSatTest {

    @Test
    fun testTrim() {
        val vars = Car()
        val cs1 = vars.mkConstraintSet1()
        assertEquals(11,cs1.satCount())
        println("satCount: ${cs1.satCount()}")
        cs1.allSat { lits, dcs ->
            println(lits.tLits)
            println("${lits.tLits}   dontCares:$dcs")
        }
        println()
    }

    @Test
    fun testSimple() {
        val vars = Simple()
        val cs1 = vars.mkConstraintSet1()
        println("satCount: ${cs1.satCount()}")
        cs1.allSat { lits, dcs ->
            println("${lits.tLits}   dontCares:$dcs")
        }
        println()
    }


}

