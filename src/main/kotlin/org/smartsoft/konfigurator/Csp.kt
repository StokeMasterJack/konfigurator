package org.smartsoft.konfigurator

class Csp(val vars: Set<Var>, val constraint: Exp, val picks: Set<Lit> = emptySet()) {

    val isFailed: Boolean get() = constraint === False

    val effectiveConstraint: Exp
        get() = when (constraint) {
            False -> False
            True -> True
            is Lit -> True
            is LitAnd -> True
            is MixedAnd -> when (constraint.constraint) {
                is ComplexAnd -> constraint.constraint
                is NonAndComplex -> constraint.constraint
                else -> throw IllegalStateException()
            }
            is ComplexAnd -> constraint
            is NonAndComplex -> constraint
        }

    val effectiveConstraints: List<Exp>
        get() {
            val c = effectiveConstraint
            return when (c) {
                is Constant -> emptyList()
                is ComplexAnd -> c.expList
                is NonAndComplex -> listOf(c)
                else -> throw IllegalStateException(c.toStringDetail())
            }
        }

    val lits: LitAnd
        get() = when (constraint) {
            False -> LitAnd.empty()
            True -> LitAnd.empty()
            is Lit -> constraint.asLitAnd
            is LitAnd -> constraint
            is MixedAnd -> constraint.lits
            is ComplexAnd -> LitAnd.empty()
            is NonAndComplex -> LitAnd.empty()
        }

    fun dontCares(): Set<Var> {
        return vars - constraint.vars
    }

    fun isSat(): Boolean {
        return constraint.isSat()
    }

    fun careVars(): Set<Var> {
        return effectiveConstraint.vars - lits.vars
    }

    fun toDnnf(): Exp {
        return constraint.toDnnf()
    }

    fun deepInfer(): Set<Lit> {
        if (constraint is Constant) LitAnd.empty()
        val careVars = careVars()
        val a = mutableSetOf<Lit>()
        for (careVar in careVars) {
            val pLit = careVar.lit(true)
            val nLit = careVar.lit(false)
            val pSat = assign(pLit).isSat()
            val nSat = assign(nLit).isSat()
            if (!pSat) a.add(pLit)
            if (!nSat) a.add(nLit)
        }
        return a
    }

    fun print() {
        println("Picks: ${picks.sorted()}")
        if (isFailed) {
            println("  Failed")
        } else {
            println("  Lits: ${lits.sorted}")
            println("  tLits: ${lits.lits1.filter { it.sign }.sorted()}")
            println("  DontCares: ${dontCares()}")
            println("  Constraint: ${effectiveConstraint.toString()}")
            println("  Constraints: ")
            for (exp in effectiveConstraints) {
                println("    $exp")
            }

            println("  isSat: ${isSat()}")
            println("  deepInfer: ${deepInfer()}")
            val dnnf = toDnnf()
            println("  dnnf: $dnnf")
            check(dnnf.isDisjointDeep())
        }

        println()


    }

    fun assign(vararg args: Lit): Csp {
        return assign(args.toSet())
    }

    fun assign(args: Set<Lit>): Csp {
        val lits = MutableLitAnd(args)
        val s = when {
            lits.isFailed() -> False
            else -> constraint.assign(lits)
        }
        return Csp(vars, s, picks + args)
    }

    fun maybeSimplify(): Csp {
        val s = constraint.maybeSimplify()
        if (s == constraint) return this
        return Csp(vars, s)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Csp) return false
        return this.vars == other.vars && this.constraint == other.constraint && this.picks == other.picks
    }

    fun checkLits(vararg expected: Lit) {
        assertEquals(expected.toSet(), lits.toSet())
    }

    fun checkDontCares(vararg expected: Var) {
        assertEquals(expected.toSet(), dontCares())
    }

    fun checkConstraint(expected: String) {
        assertEquals(expected, effectiveConstraint.toString())
    }

    fun checkConstraintEmpty() {
        check(effectiveConstraint === True)
    }

    fun checkFailed() {
        check(isFailed)
    }

    fun checkSat() {
        check(isSat())
        check(deepInfer().isEmpty())
    }

    fun checkNotSat() {
        check(!isSat())
    }

    fun assertEquals(expected: Set<Lit>, actual: Set<Lit>) {
        if (expected != actual) {
            println("expected = ${expected.toList().sorted()}")
            println("actual   = ${actual.toList().sorted()}")
            throw IllegalStateException("expected[${expected.toList().sorted()}]  actual[${actual.toList().sorted()}]")
        }
    }

    fun assertEquals(expected: String, actual: String) {
        if (expected != actual) {
            println("expected = ${expected}")
            println("actual   = ${actual}")
            throw IllegalStateException("expected[$expected]  actual[$actual]")
        }
    }

    fun allSat(callback: AllSatCallback) {
        constraint.allSat(vars, callback)
    }

    fun satCount(): Long {
        val counter = Counter()
        constraint.satCount(vars, counter)
        return counter.cnt
    }

    override fun hashCode(): Int {
        throw UnsupportedOperationException()
    }
}