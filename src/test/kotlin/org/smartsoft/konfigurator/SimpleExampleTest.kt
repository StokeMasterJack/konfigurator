package org.smartsoft.konfigurator

import org.junit.Before
import org.smartsoft.konfigurator.data.SimpleExample
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleExampleTest {

    lateinit var expFactory: SimpleExample
    lateinit var csp: Csp

    @Before
    fun setup() {
        expFactory = SimpleExample()
        csp = expFactory.buildCsp()
    }

    @Test
    fun testPosA() {
        with(expFactory) {
            assertEquals(csp, csp.maybeSimplify())
            csp.assign(a).apply {
                checkLits(a, !b, !c, d)
                checkDontCares(i)
                checkConstraint("And[Or[e, f], Requires[f, And[g, h]], Xor[green, red]]")
                checkSat()
            }.assign(!e).apply {
                checkLits(a, !b, !c, d, !e, f, g, h)
                checkDontCares(i)
                checkConstraint("Xor[green, red]")
                checkSat()
            }.assign(green).apply {
                checkLits(a, !b, !c, d, !e, f, g, h, green, !red)
                checkDontCares(i)
                checkConstraintEmpty()
                checkSat()
            }.apply {
                assertEquals(this, assign(a))
            }.assign(!a).apply {
                checkFailed()
                checkNotSat()
            }
            csp.assign(a, !e, green).apply {
                checkLits(a, !b, !c, d, !e, f, g, h, green, !red)
                checkDontCares(i)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }

    @Test
    fun testNegA() {
        with(expFactory) {
            csp.assign(!a).apply {
                checkLits(!a, !f, !green, red)
                checkDontCares(b, c, g, h, i)
                checkConstraint("Requires[d, e]")
                checkSat()
            }.assign(d).apply {
                checkLits(!a, !f, !green, red, d, e)
                checkDontCares(b, c, g, h, i)
                checkConstraintEmpty()
                checkSat()
            }.assign(a).apply {
                checkFailed()
                checkNotSat()
            }
        }


    }


    /*
    conflict(a, b)
            conflict(a, c)
            requires(a, d)
            requires(d, or(e, f))
            requires(f, and(g, h, a))
            xor(red, green)
            requires(green, a)
     */
    @Test
    fun testPosB1() {
        with(expFactory) {
            csp.assign(b).apply {
                checkLits(!a, b, !f, !green, red)
                checkDontCares(c, g, h, i)
                checkConstraint("Requires[d, e]")
                checkSat()
            }.assign(d).apply {
                checkLits(!a, b, d, e, !f, !green, red)
                checkDontCares(c, g, h, i)
                checkConstraintEmpty()
                checkSat()
            }.assign(c, g, h, i).apply {
                checkLits(!a, b, c, d, e, !f, g, !green, h, i, red)
                checkDontCares()
                checkConstraintEmpty()
                checkSat()
            }.assign(a).apply {
                checkFailed()
                checkNotSat()
            }
        }
    }

    @Test
    fun testPosB() {
        with(expFactory) {
            csp.assign(b).apply {
                checkLits(!a, b, !f, !green, red)
                checkDontCares(c, g, h, i)
                checkConstraint("Requires[d, e]")
                checkSat()
            }.assign(d).apply {
                checkLits(!a, b, d, e, !f, !green, red)
                checkDontCares(c, g, h, i)
                checkConstraintEmpty()
                checkSat()
            }.assign(c, g, h, i).apply {
                checkLits(!a, b, c, d, e, !f, g, !green, h, i, red)
                checkDontCares()
                checkConstraintEmpty()
                checkSat()
            }.assign(a).apply {
                checkFailed()
                checkNotSat()
            }
        }
    }

    @Test
    fun testNegB() {
        with(expFactory) {
            val csp1 = csp.assign(!b).apply {
                checkLits(!b)
                checkDontCares(i)
                checkConstraint("And[Conflict[a, c], Requires[a, d], Requires[green, a], Requires[d, Or[e, f]], Requires[f, And[a, g, h]], Xor[green, red]]")
                checkSat()
            }.assign(green).apply {
                checkLits(a, !b, !c, d, green, !red)
                checkDontCares(i)
                checkConstraint("And[Or[e, f], Requires[f, And[g, h]]]")
                checkSat()
            }.assign(!e).apply {
                checkLits(a, !b, !c, d, !e, f, g, h, green, !red)
                checkDontCares(i)
                checkConstraintEmpty()
                checkSat()
            }

            csp1.assign(!green).apply {
                checkFailed()
                checkNotSat()
            }

            csp.assign(!b, green, !e).apply {
                assertEquals(csp1, this)
            }
        }
    }

    @Test
    fun testPosC() {
        with(expFactory) {
            val csp1 = csp.assign(c).apply {
                checkLits(!a, c, !f, !green, red)
                checkDontCares(b, g, h, i)
                checkConstraint("Requires[d, e]")
                checkSat()
            }.assign(b, !g).apply {
                checkLits(!a, b, c, !f, !g, !green, red)
                checkDontCares(h, i)
                checkConstraint("Requires[d, e]")
                checkSat()
            }.assign(e).apply {
                checkLits(!a, b, c, e, !f, !g, !green, red)
                checkDontCares(d, h, i)
                checkConstraintEmpty()
                checkSat()
            }

            csp.assign(c, b, !g, e).apply {
                assertEquals(csp1, this)
            }
        }
    }

    @Test
    fun testNegC() {
        with(expFactory) {

            val csp1 = csp.assign(!c).apply {
                checkLits(!c)
                checkDontCares(i)
                checkConstraint("And[Conflict[a, b], Requires[a, d], Requires[green, a], Requires[d, Or[e, f]], Requires[f, And[a, g, h]], Xor[green, red]]")
                checkSat()
            }.assign(!h).apply {
                checkLits(!c, !f, !h)
                checkDontCares(i, g)
                checkConstraint("And[Conflict[a, b], Requires[a, d], Requires[green, a], Requires[d, e], Xor[green, red]]")
                checkSat()
            }.assign(d).apply {
                checkLits(!c, d, e, !f, !h)
                checkDontCares(i, g)
                checkConstraint("And[Conflict[a, b], Requires[green, a], Xor[green, red]]")
                checkSat()
            }.assign(a).apply {
                checkLits(a, !b, !c, d, e, !f, !h)
                checkDontCares(i, g)
                checkConstraint("Xor[green, red]")
                checkSat()
            }.assign(!green).apply {
                checkLits(a, !b, !c, d, e, !f, !h, !green, red)
                checkDontCares(i, g)
                checkConstraintEmpty()
                checkSat()
            }

            csp.assign(!c, !h, d, a, !green).apply {
                assertEquals(csp1, this)
                checkSat()
            }
        }
    }

    @Test
    fun testPosD() {
        with(expFactory) {

            val csp1 = csp.assign(d).apply {
                checkLits(d)
                checkDontCares(i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Or[e, f], Requires[green, a], Requires[f, And[a, g, h]], Xor[green, red]]")
                checkSat()
            }.assign(green).apply {
                checkLits(a, !b, !c, d, green, !red)
                checkDontCares(i)
                checkConstraint("And[Or[e, f], Requires[f, And[g, h]]]")
                checkSat()
            }.assign(e).apply {
                checkLits(a, !b, !c, d, e, green, !red)
                checkDontCares(i)
                checkConstraint("Requires[f, And[g, h]]")
                checkSat()
            }.assign(!g).apply {
                checkLits(a, !b, !c, d, e, !f, !g, green, !red)
                checkDontCares(i, h)
                checkConstraintEmpty()
                checkSat()
            }

            csp.assign(d, green, e, !g).apply {
                assertEquals(csp1, this)
                checkSat()
            }
        }
    }

    @Test
    fun testNegD() {
        with(expFactory) {
            csp.assign(!d).apply {
                checkLits(!a, !d, !green, !f, red)
                checkDontCares(b, c, e, g, h, i)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }

    @Test
    fun testPosE() {
        with(expFactory) {
            csp.assign(e, green).apply {
                checkLits(a, !b, !c, d, e, green, !red)
                checkDontCares(i)
                checkConstraint("Requires[f, And[g, h]]")
                checkSat()
            }.assign(g).apply {
                checkLits(a, !b, !c, d, e, g, green, !red)
                checkDontCares(i)
                checkConstraint("Requires[f, h]")
                checkSat()
            }.assign(h).apply {
                checkLits(a, !b, !c, d, e, g, green, h, !red)
                checkDontCares(f, i)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }

    @Test
    fun testNegE() {
        with(expFactory) {
            csp.assign(!e, !f).apply {
                checkLits(!a, !d, !e, !f, !green, red)
                checkDontCares(b, c, i, g, h)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }

    @Test
    fun testPosF() {
        with(expFactory) {
            csp.assign(f).apply {
                checkLits(a, !b, !c, d, f, g, h)
                checkDontCares(e, i)
                checkConstraint("Xor[green, red]")
                checkSat()
            }.assign(!green).apply {
                checkLits(a, !b, !c, d, f, g, !green, red, h)
                checkDontCares(e, i)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }

    @Test
    fun testNegF() {
        with(expFactory) {
            csp.assign(!f).apply {
                checkLits(!f)
                checkDontCares(g, h, i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Requires[a, d], Requires[green, a], Requires[d, e], Xor[green, red]]")
                checkSat()
            }.assign(a).apply {
                checkLits(a, !b, !c, d, e, !f)
                checkDontCares(g, h, i)
                checkConstraint("Xor[green, red]")
                checkSat()
            }.assign(green).apply {
                checkLits(a, !b, !c, d, e, green, !f, !red)
                checkDontCares(g, h, i)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }

    @Test
    fun testPosG() {
        with(expFactory) {
            csp.assign(g).apply {
                checkLits(g)
                checkDontCares(i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Requires[a, d], Requires[green, a], Requires[d, Or[e, f]], Requires[f, And[a, h]], Xor[green, red]]")
                checkSat()
            }.assign(h).apply {
                checkLits(g, h)
                checkDontCares(i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Requires[a, d], Requires[f, a], Requires[green, a], Requires[d, Or[e, f]], Xor[green, red]]")
                checkSat()
            }.assign(a).apply {
                checkLits(a, !b, !c, d, g, h)
                checkDontCares(i)
                checkConstraint("And[Or[e, f], Xor[green, red]]")
                checkSat()
            }.assign(!e).apply {
                checkLits(a, !b, !c, d, !e, f, g, h)
                checkDontCares(i)
                checkConstraint("Xor[green, red]")
                checkSat()
            }.assign(green).apply {
                checkLits(a, !b, !c, d, !e, f, g, green, !red, h)
                checkDontCares(i)
                checkConstraintEmpty()
                checkSat()
            }

        }
    }

    @Test
    fun testNegG() {
        with(expFactory) {
            csp.assign(!g).apply {
                checkLits(!f, !g)
                checkDontCares(h, i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Requires[a, d], Requires[green, a], Requires[d, e], Xor[green, red]]")
                checkSat()
            }.assign(!a).apply {
                checkLits(!a, !f, !g, !green, red)
                checkDontCares(b, c, h, i)
                checkConstraint("Requires[d, e]")
                checkSat()
            }.assign(e).apply {
                checkLits(!a, e, !f, !g, !green, red)
                checkDontCares(b, c, d, h, i)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }


    @Test
    fun testPosH() {
        with(expFactory) {
            csp.assign(h).apply {
                checkLits(h)
                checkDontCares(i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Requires[a, d], Requires[green, a], Requires[d, Or[e, f]], Requires[f, And[a, g]], Xor[green, red]]")
                checkSat()
            }.assign(red).apply {
                checkLits(h, !green, red)
                checkDontCares(i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Requires[a, d], Requires[d, Or[e, f]], Requires[f, And[a, g]]]")
                checkSat()
            }.assign(!g).apply {
                checkLits(!g, h, !f, !green, red)
                checkDontCares(i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Requires[a, d], Requires[d, e]]")
                checkSat()
            }.assign(b).apply {
                checkLits(!a, b, !g, h, !f, !green, red)
                checkDontCares(c, i)
                checkConstraint("Requires[d, e]")
                checkSat()
            }.assign(d).apply {
                checkLits(!a, b, d, e, !g, h, !f, !green, red)
                checkDontCares(c, i)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }

    @Test
    fun testNegH() {
        with(expFactory) {
            val csp1 = csp.assign(!h).apply {
                checkLits(!f, !h)
                checkDontCares(g, i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Requires[a, d], Requires[green, a], Requires[d, e], Xor[green, red]]")
                checkSat()
            }.assign(a).apply {
                checkLits(a, !b, !c, d, e, !f, !h)
                checkDontCares(g, i)
                checkConstraint("Xor[green, red]")
                checkSat()
            }.assign(green).apply {
                checkLits(a, !b, !c, d, e, !f, green, !h, !red)
                checkDontCares(g, i)
                checkConstraintEmpty()
                checkSat()
            }
            csp.assign(!h, a, green).apply {
                assertEquals(csp1, this)
            }
        }
    }


    @Test
    fun testPosGreen() {
        with(expFactory) {
            val csp1 = csp.assign(green).apply {
                checkLits(a, !b, !c, d, green, !red)
                checkDontCares(i)
                checkConstraint("And[Or[e, f], Requires[f, And[g, h]]]")
                checkSat()
            }.assign(!f).apply {
                checkLits(a, !b, !c, d, e, !f, green, !red)
                checkDontCares(g, h, i)
                checkConstraintEmpty()
                checkSat()
            }
            csp.assign(green, !f).apply {
                assertEquals(csp1, this)
            }
        }
    }

    @Test
    fun testNegGreen() {
        with(expFactory) {
            val csp1 = csp.assign(!green).apply {
                checkLits(!green, red)
                checkDontCares(i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Requires[a, d], Requires[d, Or[e, f]], Requires[f, And[a, g, h]]]")
                checkSat()
            }.assign(!h).apply {
                checkLits(!f, !h, !green, red)
                checkDontCares(g, i)
                checkConstraint("And[Conflict[a, b], Conflict[a, c], Requires[a, d], Requires[d, e]]")
                checkSat()
            }.assign(!a).apply {
                checkLits(!a, !f, !h, !green, red)
                checkDontCares(b, c, g, i)
                checkConstraint("Requires[d, e]")
                checkSat()
            }.assign(d).apply {
                checkLits(!a, d, e, !f, !h, !green, red)
                checkDontCares(b, c, g, i)
                checkConstraintEmpty()
                checkSat()
            }
            csp.assign(!green, !h, !a, d).apply {
                assertEquals(csp1, this)
            }
        }
    }


}

