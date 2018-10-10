package org.smartsoft.konfigurator

import org.smartsoft.konfigurator.data.CarSpace
import org.smartsoft.konfigurator.data.SimpleSpace
import kotlin.test.Test
import kotlin.test.assertEquals


class AllSatTest {

    @Test
    fun testTrim() {
        val vars = CarSpace()
        val rs1 = vars.mkRuleSet1()
        assertEquals(11,rs1.satCount())
        println("satCount: ${rs1.satCount()}")
        rs1.allSat { lits, dcs ->
            println(lits.tLits)
            println("${lits.tLits}   dontCares:$dcs")
        }
        println()
    }

    @Test
    fun testSimple() {
        val vars = SimpleSpace()
        val rs1 = vars.mkRuleSet1()
        println("satCount: ${rs1.satCount()}")
        rs1.allSat { lits, dcs ->
            println("${lits.tLits}   dontCares:$dcs")
        }
        println()
    }


}

