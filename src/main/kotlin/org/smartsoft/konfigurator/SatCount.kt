package org.smartsoft.konfigurator

import kotlin.math.roundToLong


class Counter {

    var cnt = 0L

    fun count(dcCount: Int) {
        cnt += Math.pow(2.0, dcCount.toDouble()).roundToLong()
    }

}

fun Exp.satCount(vars: Set<Var>, counter: Counter) {

    return when (this) {
        is Constant -> throw IllegalStateException()
        is Lit -> {
            println("LIT")
            val dcCount = (vars - this.vars).size
            counter.count(dcCount)
        }
        is LitAnd -> {
            val dcCount = (vars - this.vars).size
            counter.count(dcCount)
        }
        is Complex -> {

            check(this is ComplexAnd || this is NonAndComplex || this is MixedAnd)
            val dVar = chooseDecisionVar()
            val pLit = dVar.lit(true)
            val nLit = dVar.lit(false)
            val pExp = assign(pLit)
            val nExp = assign(nLit)

            pExp.satCount(vars, counter)
            nExp.satCount(vars, counter)
        }
    }


}