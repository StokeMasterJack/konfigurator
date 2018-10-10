package org.smartsoft.konfigurator

import org.junit.Before
import org.smartsoft.konfigurator.data.Simple
import kotlin.test.Test

class DnnfTest {

    lateinit var space: Simple
    lateinit var cs1: ConstraintSet
    @Before
    fun setup() {
        space = Simple()
        cs1 = space.mkConstraintSet1()
    }

    @Test
    fun test() {
        with(space) {
            println(cs1.toDnnf())
            cs1.assign(a).apply {
                println(toDnnf())
            }.assign(!e).apply {
                println(toDnnf())
            }.assign(green).apply {
                println(toDnnf())
            }.assign(!a).apply {
                println(toDnnf())
            }
        }
    }
}