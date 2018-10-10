package org.smartsoft.konfigurator


class Csp(val expFactory: ExpFactory, val constraint: Exp, val picks: Set<Lit> = emptySet()) {

    val isFailed: Boolean
        get() = constraint is False


    val effectiveConstraint: Exp
        get() = when (constraint) {
            is False -> expFactory.mkFalse()
            is True -> expFactory.mkTrue()
            is Lit -> expFactory.mkTrue()
            is LitAnd -> expFactory.mkTrue()
            is MixedAnd -> when (constraint.constraint) {
                is ComplexAnd -> constraint.constraint
                is NonAnd -> constraint.constraint
                else -> throw IllegalStateException()
            }
            is ComplexAnd -> constraint
            is NonAnd -> constraint
        }


    val effectiveConstraints: List<Exp>
        get() {
            val c = effectiveConstraint
            return when (c) {
                is Constant -> emptyList()
                is ComplexAnd -> c.exps.toList()
                is NonAnd -> listOf(c)
                else -> throw IllegalStateException(c.toStringDetail())
            }
        }


    val lits: Assignment
        get() = when (constraint) {
            is False -> LitAnd.EMPTY
            is True -> LitAnd.EMPTY
            is Lit -> LitAnd.create(constraint)
            is LitAnd -> constraint
            is MixedAnd -> constraint.lits
            is ComplexAnd -> LitAnd.EMPTY
            is NonAnd -> LitAnd.EMPTY
        }

    fun dontCares(): Set<Var> {
        return vars - constraint.vars
    }

    val vars: Set<Var> get() = expFactory.vars

    fun isSat(): Boolean {
        return constraint.isSat(expFactory)
    }

    /**
     * Compiles boolean expression into DNNF form
     */
    fun toDnnf(): Exp {
        return constraint.toDnnf(expFactory)
    }

    private fun deepInfer(c: Exp): List<Lit> {
        return if (c is Simple) {
            emptyList()
        } else if (c is LitAnd) {
            emptyList()
        } else if (c is MixedAnd) {
            if (c.disjoint) {
                deepInfer(c.constraint)
            } else {
                deepInfer(c.maybeSimplify(expFactory))
            }
        } else {
            require(c is ComplexAnd || c is NonAnd)
            val careVars = c.vars
            val inferredLits = mutableListOf<Lit>()
            for (careVar in careVars) {
                val pLit = careVar.lit(true)
                val nLit = careVar.lit(false)
                val pSat = assign(pLit).isSat()
                if (!pSat) {
                    inferredLits.add(nLit)
                } else {
                    val nSat = assign(nLit).isSat()
                    if (!nSat) {
                        inferredLits.add(pLit)
                    }
                }
            }
            inferredLits
        }

    }

    fun deepInfer(): List<Lit> {
        return deepInfer(constraint)
    }

    fun print() {
        println("Picks: ${picks.sorted()}")
        if (isFailed) {
            println("  Failed")
        } else {
            println("  Lits: ${lits.asIterable.sorted()}")
            println("  DontCares: ${dontCares()}")
            println("  Constraint: $effectiveConstraint")
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
        val lits = LitAndBuilder().assignLits(args)
        if (lits.isFailed()) throw IllegalArgumentException("Conflicting assignment: [${lits.conflictLit}]")
        val aa = lits.mkAssignment()
        val s: Exp = constraint.assign(aa, expFactory)
        val newPics: Set<Lit> = picks + args
        return Csp(expFactory, s, newPics)
    }

    fun maybeSimplify(): Csp {
        val s = constraint.maybeSimplify(expFactory)
        if (s == constraint) return this
        return Csp(expFactory, s)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Csp) return false
        return this.vars == other.vars && this.constraint == other.constraint && this.picks == other.picks
    }

    fun checkLits(vararg expected: Lit) {
        assertEquals(expected.toSet(), lits.asIterable.toSet())
    }

    fun checkDontCares(vararg expected: Var) {
        assertEquals(expected.toSet(), dontCares())
    }

    fun checkConstraint(expected: String) {
        assertEquals(expected, effectiveConstraint.toString())
    }

    fun checkConstraintEmpty() {
        check(effectiveConstraint is True)
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
            println("expected = ${expected.sorted()}")
            println("actual   = ${actual.sorted()}")
            throw IllegalStateException("expected[${expected.sorted()}]  actual[${actual.sorted()}]")
        }
    }

    fun assertEquals(expected: String, actual: String) {
        if (expected != actual) {
            println("expected = $expected")
            println("actual   = $actual")
            throw IllegalStateException("expected[$expected]  actual[$actual]")
        }
    }

    fun allSat(callback: AllSatCallback) {
        constraint.allSat(vars, callback, expFactory)
    }

    fun satCount(): Long {
        val counter = Counter()
        constraint.satCount(vars, counter, expFactory)
        return counter.cnt
    }

    override fun hashCode(): Int {
        throw UnsupportedOperationException()
    }
}