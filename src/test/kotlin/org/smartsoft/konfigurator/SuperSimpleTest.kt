package org.smartsoft.konfigurator

import org.smartsoft.konfigurator.data.Simple
import kotlin.test.Test

class SuperSimpleTest {

    @Test
    fun test() {

        Simple().run {
            mkConstraintSet1().apply {
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

        val vars: Simple = Simple()
        val cs1: ConstraintSet = vars.mkConstraintSet1().apply { print() }
        val cs2 = cs1.assign(vars.a).apply { print() }
        val cs3 = cs2.assign(vars.f).apply { print() }
        val cs4 = cs3.assign(vars.red).apply { print() }
        val cs5 = cs4.assign(vars.i).apply { print() }
        val cs6 = cs5.assign(!vars.g).apply { print() }


    }

    @Test
    fun test2() {

        Simple().run {
            mkConstraintSet2().apply {
                print()
            }.assign(a).apply {
                print()
            }.assign(red).apply {
                print()
            }

        }


    }


}

