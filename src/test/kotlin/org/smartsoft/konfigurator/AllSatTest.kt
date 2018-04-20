package org.smartsoft.konfigurator

import org.smartsoft.konfigurator.data.CarExample
import org.smartsoft.konfigurator.data.SimpleExample
import kotlin.test.Test
import kotlin.test.assertEquals


class AllSatTest {

    @Test
    fun testTrim() {
        val vars = CarExample()
        val csp = vars.buildCsp()
        assertEquals(11,csp.satCount())
        println("satCount: ${csp.satCount()}")
        csp.allSat { lits, dcs ->
            println(lits.tLits)
            println("${lits.tLits}   dontCares:$dcs")
        }
        println()
    }

    @Test
    fun testSimple() {
        val vars = SimpleExample()
        val csp = vars.buildCsp()
        println("satCount: ${csp.satCount()}")
        csp.allSat { lits, dcs ->
            println("${lits.tLits}   dontCares:$dcs")
        }
        println()
    }


}

