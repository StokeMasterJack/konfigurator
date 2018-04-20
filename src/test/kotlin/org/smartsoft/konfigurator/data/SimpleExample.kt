package org.smartsoft.konfigurator.data

import org.smartsoft.konfigurator.Csp
import org.smartsoft.konfigurator.ExpFactory

class SimpleExample : ExpFactory() {

    val a = +"a"
    val b = +"b"
    val c = +"c"
    val d = +"d"
    val e = +"e"
    val f = +"f"
    val g = +"g"
    val h = +"h"
    val i = +"i"
    val red = +"red"
    val green = +"green"

    fun buildCsp(): Csp {
        conflict(a, b)
        conflict(a, c)
        requires(a, d)
        requires(d, or(e, f))
        requires(f, and(g, h, a))
        xor(red, green)
        requires(green, a)
        return mkCsp()
    }



}