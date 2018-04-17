package org.smartsoft.konfigurator


class AndBuilder {

    val lits: MutableLitAnd = MutableLitAnd()
    val complex: MutableComplexAnd = MutableComplexAnd()

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
            e === False -> {
                isShortCircuit = true
                return
            }
            e === True -> {
                return //skip
            }
        }

        val s = if (assignments != null) {
            val ss = e.maybeSimplify(assignments)
            ss
        } else {
            e
        }

        when (s) {
            True -> {
                //do nothing
            }
            False -> {
                isShortCircuit = true
            }
            is Lit -> {
                check(s !is Constant)
                lits.assignLitInPlace(s)
            }
            is And -> {
                for (ss in s.exps) {
                    if (isFailed) continue
                    add(ss)
                }
            }
            is Complex -> {
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

    fun isDisjoint(): Boolean {
        if (!isMixed) return false
        return lits.isDisjoint(complex)
    }

    override fun toString(): String {
        return "AndBuilder[$lits, $complex]"
    }
}