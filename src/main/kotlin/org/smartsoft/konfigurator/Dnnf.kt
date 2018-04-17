package org.smartsoft.konfigurator

fun Exp.toDnnf(): Exp {
    return when (this) {
        is Constant -> this
        is Lit -> this
        is LitAnd -> this
        is ComplexAnd -> dnnfSplit()
        is MixedAnd -> {
            if (!disjoint) {
                return maybeSimplify().toDnnf()
            }
            expFactory.mkAndDisjoint(constraint.toDnnf(), lits)
        }
        is NonAndComplex -> dnnfSplit()
    }
}

private fun Complex.dnnfSplit(): Exp {
    require(this is ComplexAnd || this is NonAndComplex)
    val d = chooseDecisionVar()
    val pLit = d.lit(true)
    val nLit = d.lit(false)
    val pExp = assign(pLit).toDnnf()
    val nExp = assign(nLit).toDnnf()
    return expFactory.mkXor(pExp, nExp)
}