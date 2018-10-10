package org.smartsoft.konfigurator


class ConstraintSet(val set: VarSet, val constraint: Exp, val userPics: Set<Lit> = emptySet()) {

    val isFailed: Boolean
        get() = constraint is False


    val effectiveConstraint: Exp
        get() = when (constraint) {
            is False -> set.mkFalse()
            is True -> set.mkTrue()
            is Lit -> set.mkTrue()
            is LitAnd -> set.mkTrue()
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

    val inferredPics: Iterable<Lit>
        get() {
            return lits.asIterable.filter { !userPics.contains(it) }
        }

    fun dontCares(): Set<Var> {
        return vars - constraint.vars
    }

    val vars: Set<Var> get() = set.vars

    fun isSat(): Boolean {
        return constraint.isSat(set)
    }

    /**
     * Compiles boolean expression into DNNF form
     */
    fun toDnnf(): Exp {
        return constraint.toDnnf(set)
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
                deepInfer(c.maybeSimplify(set))
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
        println("User Pics:    ${userPics.sorted()}")
        if (isFailed) {
            println("Constraints: FAILED!!")
        } else {
            println("Inferred Pics: ${inferredPics.sorted()}")
            println("Unconstrained: ${dontCares()}")
            println("Constraints: ")
            for (exp in effectiveConstraints) {
                println("  $exp")
            }
        }
        println()
    }

    fun printFull() {
        println("User Picks:    ${userPics.sorted()}")
        if (isFailed) {
            println("Failed")
        } else {
            println("Inferred Pics: ${inferredPics.sorted()}")
            println("Unconstrained: ${dontCares()}")
//            println("  Constraint: $effectiveConstraint")
            println("Constraints: ")
            for (exp in effectiveConstraints) {
                println("  $exp")
            }
            println("  isSat: ${isSat()}")
//            println("  deepInfer: ${deepInfer()}")
            val dnnf = toDnnf()
            println("  dnnf: $dnnf")
            check(dnnf.isDisjointDeep())
        }
        println()
    }

    fun assign(vararg args: Lit): ConstraintSet {
        return assign(args.toSet())
    }

    fun assign(args: Set<Lit>): ConstraintSet {
        val lits = LitAndBuilder().assignLits(args)
        if (lits.isFailed()) throw IllegalArgumentException("Conflicting assignment: [${lits.conflictLit}]")
        val aa = lits.mkAssignment()
        val s: Exp = constraint.assign(aa, set)
        val newPics: Set<Lit> = userPics + args
        return ConstraintSet(set, s, newPics)
    }

    fun maybeSimplify(): ConstraintSet {
        val s = constraint.maybeSimplify(set)
        if (s == constraint) return this
        return ConstraintSet(set, s)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is ConstraintSet) return false
        return this.vars == other.vars && this.constraint == other.constraint && this.userPics == other.userPics
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
        constraint.allSat(vars, callback, set)
    }

    fun satCount(): Long {
        val counter = Counter()
        constraint.satCount(vars, counter, set)
        return counter.cnt
    }

    override fun hashCode(): Int {
        throw UnsupportedOperationException()
    }
}