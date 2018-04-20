package org.smartsoft.konfigurator


class AndBuilder(val expFactory: ExpFactory) {

    val lits: LitAndBuilder = LitAndBuilder()
    val complex: ComplexAndBuilder = ComplexAndBuilder()

    var isShortCircuit: Boolean = false

    fun addAll(exps: Iterable<Exp>, assignments: Assignment? = null) {
        for (e in exps) {
            add(e, assignments)
            if (isFailed) return
        }
    }

    val isFailed: Boolean
        get() {
            return isShortCircuit || lits.isFailed()
        }

    fun add(e: Exp, assignments: Assignment? = null) {
        if (isShortCircuit) throw IllegalStateException()
        when {
            isShortCircuit -> throw IllegalStateException()
            e is False -> {
                isShortCircuit = true
                return
            }
            e is True -> {
                return //skip
            }
        }

        val s = if (assignments != null) {
            val ss = e.maybeSimplify(assignments, expFactory)
            ss
        } else {
            e
        }

        when (s) {
            is True -> {
                //do nothing
            }
            is False -> {
                isShortCircuit = true
            }
            is Lit -> {
                check(s !is Constant)
                lits.assign(s)
            }
            is And -> {
                for (ss in s.exps) {
                    if (isFailed) continue
                    add(ss)
                }
            }
            is NonAnd -> {
                check(s !is And)
                check(s !is Constant)
                complex.add(s)
            }
        }
    }

    private fun isLitsEmpty(): Boolean {
        return lits.isEmpty()
    }

    private fun isComplexEmpty(): Boolean {
        return complex.isEmpty()
    }

    fun isEmpty() = isLitsEmpty() && isComplexEmpty()

    val isPureLits: Boolean get() = lits.isNotEmpty() && complex.isEmpty()
    val isPureComplex: Boolean get() = lits.isEmpty() && complex.isNotEmpty()
    val isMixed: Boolean get() = lits.isNotEmpty() && complex.isNotEmpty()

    override fun toString(): String {
        return "AndBuilder[$lits, $complex]"
    }

    fun mk(): Exp {
        return when {
            isFailed -> {
                expFactory.mkFalse()
            }
            isEmpty() -> {
                expFactory.mkTrue()
            }
            isPureLits -> {
                lits.mk(expFactory)
            }
            isPureComplex -> {
                complex.mk(expFactory)
            }
            isMixed -> {
                val e1 = complex.mk(expFactory) as Complex
                val e2 = lits.mk(expFactory) as Assignment
                val disjoint = e1.isDisjoint(e2)
                expFactory.mk(MixedAnd(e1, e2, disjoint))
            }
            else -> {
                throw IllegalStateException()
            }
        }
    }
}