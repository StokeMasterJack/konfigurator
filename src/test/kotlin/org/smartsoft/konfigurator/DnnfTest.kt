package org.smartsoft.konfigurator

import org.junit.Before
import org.smartsoft.konfigurator.data.SimpleExample
import kotlin.test.Test

class DnnfTest {

    lateinit var vars: SimpleExample
    lateinit var csp: Csp
    @Before
    fun setup() {
        vars = SimpleExample()
        csp = vars.buildCsp()
    }

    @Test
    fun test() {
        with(vars) {
            println(csp.toDnnf())
            csp.assign(a).apply {
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