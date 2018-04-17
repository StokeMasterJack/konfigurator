package org.smartsoft.konfigurator

fun Exp.isSat(): Boolean {
    return when (this) {
        False -> false
        True -> true
        is Lit -> true
        is LitAnd -> !isFailed()
        is MixedAnd -> {
            if (disjoint) {
                constraint.isSat()
            } else {
                constraint.maybeSimplify().isSat()
            }
        }
        is Complex -> {
            check(this is ComplexAnd || this is NonAndComplex)
            val pLit = this.chooseDecisionVar()
            val nLit = pLit.neg
            val pExp = assign(pLit)
            return if (pExp.isSat()) {
                true
            } else {
                val nExp = assign(nLit)
                nExp.isSat()
            }
        }
    }
}