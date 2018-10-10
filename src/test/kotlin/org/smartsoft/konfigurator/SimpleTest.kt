package org.smartsoft.konfigurator

import org.junit.Before
import org.smartsoft.konfigurator.data.Simple
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleTest {

    lateinit var space: Simple
    lateinit var cs1: ConstraintSet

    @Before
    fun setup() {
        space = Simple()
        cs1 = space.mkConstraintSet1()
    }

    @Test
    fun testPosA() {
        with(space) {
            assertEquals(cs1, cs1.maybeSimplify())
            cs1.assign(a).apply {
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
            cs1.assign(a, !e, green).apply {
                checkLits(a, !b, !c, d, !e, f, g, h, green, !red)
                checkDontCares(i)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }

    @Test
    fun testNegA() {
        with(space) {
            cs1.assign(!a).apply {
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

    @Test
    fun testPosB1() {
        with(space) {
            cs1.assign(b).apply {
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
        with(space) {
            cs1.assign(b).apply {
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
        with(space) {
            val rs1a = cs1.assign(!b).apply {
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

            rs1a.assign(!green).apply {
                checkFailed()
                checkNotSat()
            }

            cs1.assign(!b, green, !e).apply {
                assertEquals(rs1a, this)
            }
        }
    }

    @Test
    fun testPosC() {
        with(space) {
            val rs1a = cs1.assign(c).apply {
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

            cs1.assign(c, b, !g, e).apply {
                assertEquals(rs1a, this)
            }
        }
    }

    @Test
    fun testNegC() {
        with(space) {

            val rs1a = cs1.assign(!c).apply {
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

            cs1.assign(!c, !h, d, a, !green).apply {
                assertEquals(rs1a, this)
                checkSat()
            }
        }
    }

    @Test
    fun testPosD() {
        with(space) {

            val rs1a = cs1.assign(d).apply {
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

            cs1.assign(d, green, e, !g).apply {
                assertEquals(rs1a, this)
                checkSat()
            }
        }
    }

    @Test
    fun testNegD() {
        with(space) {
            cs1.assign(!d).apply {
                checkLits(!a, !d, !green, !f, red)
                checkDontCares(b, c, e, g, h, i)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }

    @Test
    fun testPosE() {
        with(space) {
            cs1.assign(e, green).apply {
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
        with(space) {
            cs1.assign(!e, !f).apply {
                checkLits(!a, !d, !e, !f, !green, red)
                checkDontCares(b, c, i, g, h)
                checkConstraintEmpty()
                checkSat()
            }
        }
    }

    @Test
    fun testPosF() {
        with(space) {
            cs1.assign(f).apply {
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
        with(space) {
            cs1.assign(!f).apply {
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
        with(space) {
            cs1.assign(g).apply {
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
        with(space) {
            cs1.assign(!g).apply {
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
        with(space) {
            cs1.assign(h).apply {
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
        with(space) {
            val rs1a = cs1.assign(!h).apply {
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
            cs1.assign(!h, a, green).apply {
                assertEquals(rs1a, this)
            }
        }
    }


    @Test
    fun testPosGreen() {
        with(space) {
            val rs1a = cs1.assign(green).apply {
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
            cs1.assign(green, !f).apply {
                assertEquals(rs1a, this)
            }
        }
    }

    @Test
    fun testNegGreen() {
        with(space) {
            val rs1a = cs1.assign(!green).apply {
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
            cs1.assign(!green, !h, !a, d).apply {
                assertEquals(rs1a, this)
            }
        }
    }


}

