package org.smartsoft.konfigurator

open class ExpFactory {

    private val map = mutableMapOf<VarId, Var>()

    fun mkVar(id: VarId): Var {
        var v = map[id]
        if (v == null) {
            v = Var(this, id)
            map[id] = v
        }
        return v
    }

    operator fun String.unaryPlus(): Var = mkVar(this)


    fun mkOr(exps: List<Exp>): Exp {
        return Or(exps).maybeSimplify()
    }

    fun mkXor(opens: List<Exp>): Exp {
        require(opens.size > 1)
        return Xor(opens).maybeSimplify()
    }

    fun mkXor(e1: Exp, e2: Exp): Exp {
        require(e1 !is Constant)
        require(e2 !is Constant)
        return mkXor(listOf(e1, e2))
    }

    fun mkConflict(e1: Exp, e2: Exp): Conflict {
        return Conflict(e1, e2)
    }

    fun mkIff(e1: Exp, e2: Exp): Iff {
        return Iff(e1, e2)
    }

    fun mkRequire(e1: Exp, e2: Exp): Requires {
        return Requires(e1, e2)
    }

    fun mkNot(complex: Complex): Exp {
        return Not(complex)
    }

    fun mkAnd(a: Assignment, lit: Lit): Exp {
        return when (a) {
            is Lit -> mkAnd(a, lit)
            is LitAnd -> mkAnd(a, lit)
            else -> throw IllegalStateException()
        }
    }

    fun mkAnd(e1: Exp, e2: Exp): Exp {
        return if (e1 === False || e2 === False) False
        else if (e2 === True) e1
        else if (e1 === True) e2
        else if (e1 is Lit) {
            e2.assign(e1)
        } else if (e1 is LitAnd) {
            e2.assign(e1)
        } else if (e2 is Lit) {
            e1.assign(e2)
        } else if (e2 is LitAnd) {
            e1.assign(e2)
        } else {
            val b = AndBuilder()
            b.add(e1)
            b.add(e2)
            return mkAnd(b)
        }
    }

    fun mkAnd(lit1: Lit, lit2: Lit): Exp {
        return if (lit1.vr !== lit2.vr) {
            MutableLitAnd().apply {
                addUnsafe(lit1)
                addUnsafe(lit2)
            }
        } else {
            if (lit1.sign == lit2.sign) {
                check(lit1 === lit2)
                lit1
            } else {
                False
            }
        }
    }

    fun mkAnd(lits: LitAnd, lit: Lit): Exp {
        return lits.assignLit(lit).maybeSimplify()
    }

    fun mkAnd(lits: LitAnd, a: Assignment): Exp = when (a) {
        is Lit -> mkAnd(lits, a)
        is LitAnd -> mkAnd(lits, a)
        else -> throw IllegalStateException()
    }

    fun mkAnd(lits1: LitAnd, lits2: LitAnd): Exp {
        return lits1.assignLitAnd(lits2).maybeSimplify()
    }

    fun mkAnd(exps: Iterable<Exp>, assignments: Assignment? = null): Exp {
        val b = AndBuilder()
        b.addAll(exps, assignments)
        return mkAnd(b)
    }

    fun mkAnd(b: AndBuilder): Exp {

        return when {
            b.isFailed -> {
                False
            }
            b.isEmpty() -> {
                True
            }
            b.isPureLits -> {
                b.lits.maybeSimplify()
            }
            b.isPureComplex -> {
                b.complex.maybeSimplify()
            }
            b.isMixed -> {
                check(b.lits.isNotEmpty())
                return mkMixedAnd(b.complex, b.lits).maybeSimplify()
            }
            else -> {
                throw IllegalStateException()
            }
        }
    }

    fun mkMixedAndDisjoint(constraint: ComplexAnd, lits: LitAnd): Exp {
        if (lits.isFailed()) return False
        if (lits.isEmpty()) return constraint
        return MixedAnd(constraint, lits, true)
    }

    fun mkMixedAndDisjoint(constraint: NonAndComplex, lits: LitAnd): Exp {
        require(lits.isDisjoint(constraint))
        if (lits.isFailed()) return False
        if (lits.isEmpty()) return constraint
        return MixedAnd(constraint, lits, true)
    }

    fun mkAndDisjoint(constraint: LitAnd, lits: LitAnd): Exp {
        if (constraint.isFailed() || lits.isFailed()) return False
        if (constraint.isEmpty() || lits.isEmpty()) return True
        return MutableLitAnd().apply {
            addAllUnsafe(constraint)
            addAllUnsafe(lits)
        }.maybeSimplify()
    }

    fun mkAndDisjoint(constraint: LitAnd, lit: Lit): Exp {
        if (constraint.isFailed()) return False
        if (constraint.isEmpty()) return lit
        return MutableLitAnd().apply {
            addAllUnsafe(constraint)
            addUnsafe(lit)
        }.maybeSimplify()
    }

    fun mkAndDisjoint(lit1: Lit, lit2: Lit): Exp {
        return MutableLitAnd().apply {
            addUnsafe(lit1)
            addUnsafe(lit2)
        }.maybeSimplify()
    }

    fun mkAndDisjoint(constraint: Exp, lits: LitAnd): Exp {
        require(lits.isDisjoint(constraint))
        return when (constraint) {
            False -> {
                False
            }
            True -> {
                lits
            }
            is Lit -> {
                mkAndDisjoint(lits, constraint)
            }
            is LitAnd -> {
                mkAndDisjoint(constraint, lits)
            }
            is MixedAnd -> {
                if (constraint.disjoint) {
                    val litsNew = MutableLitAnd().apply {
                        addAllUnsafe(constraint.lits)
                        addAllUnsafe(lits)
                    }
                    mkAndDisjoint(constraint.constraint, litsNew)
                } else {
                    mkAndDisjoint(constraint.maybeSimplify(), lits)
                }
            }
            is ComplexAnd -> {
                mkMixedAndDisjoint(constraint, lits)
            }
            is NonAndComplex -> {
                mkMixedAndDisjoint(constraint, lits)
            }


        }
    }

    fun mkAndDisjoint(lits1: Assignment, lits2: Assignment): MutableLitAnd {
        return MutableLitAnd.mkAndDisjoint(lits1, lits2)
    }

    fun mkMixedAnd(constraint: Complex, lits: LitAnd, disjoint: Boolean? = null): Exp {
        return when {
            lits.isFailed() -> False
            lits.isEmpty() -> constraint.maybeSimplify()
            else -> {
                val isDisjoint = disjoint ?: lits.isDisjoint(constraint)
                if (isDisjoint) {
                    mkAndDisjoint(constraint.maybeSimplify(), lits)
                } else {
                    Propagator.propagate(this, constraint, lits)
                }
            }
        }

    }


    private val constraints = mutableListOf<Exp>()

    fun add(e: Exp) {
        constraints.add(e)
    }

    fun conflict(e1: Exp, e2: Exp) {
        add(mkConflict(e1, e2))
    }

    fun iff(e1: Exp, e2: Exp) {
        add(mkIff(e1, e2))
    }

    fun requires(e1: Exp, e2: Exp) {
        add(mkRequire(e1, e2))
    }

    fun xor(vararg exps: Exp) {
        add(mkXor(exps.toList()))
    }

    fun and(vararg exps: Exp): Exp {
        return mkAnd(exps.toList())
    }

    fun or(vararg exps: Exp): Exp {
        return mkOr(exps.toList())
    }

    fun mkCsp(): Csp {
        val exp = mkAnd(constraints)
        return Csp(map.values.toSet(), exp)
    }


}