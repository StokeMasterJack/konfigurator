package org.smartsoft.konfigurator

import org.junit.Before
import org.smartsoft.konfigurator.data.SimpleSpace
import kotlin.test.Test

class DnnfTest {

    lateinit var space: SimpleSpace
    lateinit var rs1: RuleSet
    @Before
    fun setup() {
        space = SimpleSpace()
        rs1 = space.mkRuleSet1()
    }

    @Test
    fun test() {
        with(space) {
            println(rs1.toDnnf())
            rs1.assign(a).apply {
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