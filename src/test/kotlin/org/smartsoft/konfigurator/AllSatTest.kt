package org.smartsoft.konfigurator

import org.smartsoft.konfigurator.data.SimpleExample
import org.smartsoft.konfigurator.data.CarExample
import kotlin.test.Test


class AllSatTest {

    @Test
    fun testTrim() {
        val vars = CarExample()
        val csp = vars.buildCsp()
        println("satCount: ${csp.satCount()}")
        csp.allSat { lits, dcs ->
            println(lits.lits1.filter { it.sign })
            println("${lits.toList()}   dontCares:${dcs}")
        }
        println()
    }

    @Test
    fun testSimple() {
        val vars = SimpleExample()
        val csp = vars.buildCsp()
        println("satCount: ${csp.satCount()}")
        csp.allSat { lits, dcs ->
            println("${lits.toList()}   dontCares:${dcs}")
        }
        println()
    }


}

