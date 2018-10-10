package org.smartsoft.konfigurator.data

import org.smartsoft.konfigurator.VarSpace


class SimpleSpace : VarSpace() {

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

    fun mkRuleSet1() = mkRuleSet(
            conflict(a, b),
            conflict(a, c),
            requires(a, d),
            requires(d, or(e, f)),
            requires(f, and(g, h, a)),
            xor(red, green),
            requires(green, a))


    fun mkRuleSet2() = mkRuleSet(
            xor(a, b, c, d, e, f, g),
            xor(green, red))

}