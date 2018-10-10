package org.smartsoft.konfigurator


open class VarSpace(var _vars: HashSet<Var>? = null) {

    private val map: MutableMap<VarId, Var> = mutableMapOf()

    val vars: HashSet<Var>
        get() {
            if (_vars == null) {
                _vars = HashSet<Var>().apply {
                    addAll(map.values)
                }
            }
            return _vars!!
        }


    private val constantTrue: True by lazy { True() }
    private val constantFalse: False by lazy { False() }

    fun mkTrue(): True = constantTrue
    fun mkFalse(): False = constantFalse

    @Suppress("MemberVisibilityCanBePrivate")
    fun mkVar(id: VarId): Var {
        if (_vars != null) throw IllegalStateException()
        var v = map[id]
        if (v == null) {
            v = Var(id)
            map[id] = v
        }
        return v
    }

    operator fun String.unaryPlus(): Var = mkVar(this)

    fun mkOr(exps: List<Exp>): Exp {
        return Or(exps).maybeSimplify(this)
    }

    fun mkXor(opens: List<Exp>): Exp {
        require(opens.size > 1)
        return Xor(opens).maybeSimplify(this)
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

    fun mkAnd(e1: Assignment, e2: Assignment): Exp {
        if (e1.isEmpty()) e2.asExp
        if (e2.isEmpty()) e1.asExp
        val b = LitAndBuilder()
        b.assign(e1)
        b.assign(e2)
        return mk(b.mk())
    }

    fun mkAnd(e1: Exp, e2: Exp): Exp {
        return if (e1 is Assignment && e2 is Assignment) {
            mkAnd(e1 as Assignment, e2 as Assignment)
        } else {
            val b = AndBuilder(this)
            b.add(e1)
            b.add(e2)
            b.mk()
        }
    }

    fun mkAnd(exps: Iterable<Exp>, assignments: Assignment? = null): Exp {
        val b = AndBuilder(this)
        b.addAll(exps, assignments)
        return b.mk()
    }

    fun mkAnd(exps: Iterable<Exp>): Exp {
        val b = AndBuilder(this)
        b.addAll(exps)
        return b.mk()
    }

    fun mkAndDisjoint(e1: Exp, e2: Exp): Exp {
        return when (e2) {
            is False -> mkFalse()
            is True -> e1
            is Lit -> mkAndDisjoint(e1, e2 as Assignment)
            is LitAnd -> mkAndDisjoint(e1, e2 as Assignment)
            is ComplexAnd -> throw UnsupportedOperationException()
            is NonAnd -> throw UnsupportedOperationException()
            is MixedAnd -> throw UnsupportedOperationException()
        }
    }

    fun mkAndDisjoint(e1: Exp, a: Assignment): Exp {
        return when (e1) {
            is False -> mkFalse()
            is True -> a.asExp
            is Lit -> mkAndDisjoint(e1 as Assignment, a)
            is LitAnd -> mkAndDisjoint(e1 as Assignment, a)
            is ComplexAnd -> mkAndDisjoint(e1, a)
            is NonAnd -> mkAndDisjoint(e1, a)
            is MixedAnd -> mkAndDisjoint(e1, a)
        }
    }

    fun mkAndDisjoint(e1: Assignment, e2: Assignment): Exp {
        require(e1.asExp.isDisjoint(e2))
        if (e1.isEmpty()) e2.asExp
        if (e2.isEmpty()) e1.asExp
        val mm = LitAndBuilder()
        mm.addUnsafe(e1)
        mm.addUnsafe(e2)
        return mm.mk(this)
    }

    private fun mkAndDisjoint(constraint: ComplexAnd, lits: Assignment): Exp {
        require(constraint.isDisjoint(lits))
        if (lits.isEmpty()) return constraint.maybeSimplify(this)
        return if (constraint.size == 1) {
            mkAndDisjoint(constraint.first(), lits)
        } else {
            mk(MixedAnd(constraint, lits, true))
        }
    }

    private fun mkAndDisjoint(constraint: NonAnd, lits: Assignment): Exp {
        require(constraint.isDisjoint(lits))
        if (lits.isEmpty()) return constraint
        return mk(MixedAnd(constraint, lits, true))
    }

    private fun mkAndDisjoint(constraint: MixedAnd, lits: Assignment): Exp {
        require(constraint.isDisjoint(lits))
        return when {
            lits.isEmpty() -> constraint
            constraint.disjoint -> {
                val newLits = mkAndDisjoint(lits, constraint.lits)
                mkAndDisjoint(constraint.constraint, newLits)
            }
            else -> {
                val newLits = mkAndDisjoint(lits, constraint.lits)
                mkAnd(constraint.constraint, newLits)
            }
        }
    }

    fun conflict(e1: Exp, e2: Exp): Exp = mkConflict(e1, e2)

    fun iff(e1: Exp, e2: Exp): Exp = mkIff(e1, e2)

    fun requires(e1: Exp, e2: Exp): Exp = mkRequire(e1, e2)

    fun xor(vararg exps: Exp): Exp = mkXor(exps.toList())

    fun and(vararg exps: Exp): Exp = mkAnd(exps.toList())

    fun or(vararg exps: Exp): Exp = mkOr(exps.toList())

//    fun mkRuleSet(constraints: List<Exp>): RuleSet {
//        val exp: Exp = mkAnd(constraints)
//        return RuleSet(this, exp)
//    }

    fun <T : Exp> mk(exp: T): T {
        return exp
    }

//    fun Iterable<Exp>.mkRuleSet(): RuleSet {
//        val constraints: Iterable<Exp> = this@mkRuleSet
//        val space: VarSpace = this@VarSpace
//        return RuleSet(space = space, constraint = mkAnd(constraints))
//    }

    fun mkRuleSet(vararg elements: Exp): RuleSet {
        val constraints: List<Exp> = if (elements.isNotEmpty()) elements.asList() else emptyList()
        return RuleSet(space = this, constraint = mkAnd(constraints))
    }

}