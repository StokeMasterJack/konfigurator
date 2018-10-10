package org.smartsoft.konfigurator

fun Exp.isSat(f: VarSet): Boolean {
    return when (this) {
        is False -> false
        is True -> true
        is Lit -> true
        is LitAnd -> true
        is MixedAnd -> {
            if (disjoint) {
                constraint.isSat(f)
            } else {
                constraint.maybeSimplify(f).isSat(f)
            }
        }
        is Complex -> {
            check(this is ComplexAnd || this is NonAnd)
            val pLit = this.chooseDecisionVar()
            val nLit = pLit.neg
            val pExp = assign(pLit, f)
            return if (pExp.isSat(f)) {
                true
            } else {
                val nExp = assign(nLit, f)
                nExp.isSat(f)
            }
        }
    }
}