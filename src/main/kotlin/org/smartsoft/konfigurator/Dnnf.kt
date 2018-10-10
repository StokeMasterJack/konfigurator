package org.smartsoft.konfigurator

fun Exp.toDnnf(f: VarSpace): Exp {
    return when (this) {
        is Constant -> this
        is Lit -> this
        is LitAnd -> this
        is ComplexAnd -> dnnfSplit(f)
        is MixedAnd -> {

            /*
             assignments.assignInPlace(cur.lits)
                        if (!cur.disjoint) {
                            queue.add(cur.overlapLits())
                        }
                        cur.constraint
             */
            if (!disjoint) {
                return maybeSimplify(f).toDnnf(f)
            }
            f.mkAndDisjoint(constraint.toDnnf(f), lits)
        }
        is NonAnd -> dnnfSplit(f)
    }
}

private fun Complex.dnnfSplit(f: VarSpace): Exp {
    require(this is ComplexAnd || this is NonAnd)
    val d = chooseDecisionVar()
    val pLit = d.lit(true)
    val nLit = d.lit(false)
    val pExp = assign(pLit, f).toDnnf(f)
    val nExp = assign(nLit, f).toDnnf(f)
    return f.mkXor(pExp, nExp)
}