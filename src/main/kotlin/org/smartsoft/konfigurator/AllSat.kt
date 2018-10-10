package org.smartsoft.konfigurator

typealias AllSatCallback = (solution: LitAnd, dontCars: Set<Var>) -> Unit

fun Exp.allSat(vars: Set<Var>, callback: AllSatCallback, f: VarSpace) {

    return when (this) {
        is Constant -> throw IllegalStateException()
        is Lit -> {
            callback(LitAnd.create(this), vars - this.vars)
        }
        is LitAnd -> {
            callback(this, vars - this.vars)
        }
        is Complex -> {
            val dVar = chooseDecisionVar()
            val pLit = dVar.lit(true)
            val nLit = dVar.lit(false)
            val pExp = assign(pLit, f)
            val nExp = assign(nLit, f)

            pExp.allSat(vars, callback, f)
            nExp.allSat(vars, callback, f)
        }
    }


}