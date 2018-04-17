package org.smartsoft.konfigurator


typealias AllSatCallback = (solution: LitAnd, dontCars: Set<Var>) -> Unit

fun Exp.allSat(vars: Set<Var>, callback: AllSatCallback) {

    return when (this) {
        is Constant -> throw IllegalStateException()
        is Lit -> {
            println("LIT")
            callback(this.asLitAnd, vars - this.vars)
        }
        is LitAnd -> {
            callback(this.lits, vars - this.vars)
        }
        is Complex -> {

            check(this is ComplexAnd || this is NonAndComplex || this is MixedAnd)
            val dVar = chooseDecisionVar()
            val pLit = dVar.lit(true)
            val nLit = dVar.lit(false)
            val pExp = assign(pLit)
            val nExp = assign(nLit)

//            if (this is MixedAnd) {
//                println("MixedAnd: $this")
//                println("  pExp: ${pExp::class}  $pExp")
//                println("  nExp: ${nExp::class}  $nExp")
//            }

            pExp.allSat(vars, callback)
            nExp.allSat(vars, callback)
        }
    }


}