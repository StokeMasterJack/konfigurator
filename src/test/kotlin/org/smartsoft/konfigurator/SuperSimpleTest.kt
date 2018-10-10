package org.smartsoft.konfigurator

import org.smartsoft.konfigurator.data.SimpleSpace
import kotlin.test.Test

class SuperSimpleTest {

    @Test
    fun test() {

        SimpleSpace().run {
            mkRuleSet1().apply {
                print()
            }.assign(a).apply {
                print()
            }.assign(f).apply {
                print()
            }.assign(red).apply {
                print()
            }.assign(green).apply {
                print()
            }

        }


    }

    @Suppress("RedundantExplicitType", "UNUSED_VARIABLE")
    @Test
    fun testLowTech() {

        val vars: SimpleSpace = SimpleSpace()
        val rs1: RuleSet = vars.mkRuleSet1().apply { print() }
        val rs2 = rs1.assign(vars.a).apply { print() }
        val rs3 = rs2.assign(vars.f).apply { print() }
        val rs4 = rs3.assign(vars.red).apply { print() }
        val rs5 = rs4.assign(vars.i).apply { print() }
        val rs6 = rs5.assign(!vars.g).apply { print() }


    }

    @Test
    fun test2() {

        SimpleSpace().run {
            mkRuleSet2().apply {
                print()
            }.assign(a).apply {
                print()
            }.assign(red).apply {
                print()
            }

        }


    }


}

