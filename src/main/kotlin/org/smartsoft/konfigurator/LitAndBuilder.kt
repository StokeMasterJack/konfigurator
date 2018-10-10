package org.smartsoft.konfigurator

class LitAndBuilder(private val map: MutableMap<Var, Boolean> = mutableMapOf()) : MutableMap<Var, Boolean> by map {

    companion object {
        fun buildMap(a: Assignment?): MutableMap<Var, Boolean> {
            val mm: MutableMap<Var, Boolean> = mutableMapOf()
            when (a) {
                null -> Unit
                is Lit -> mm[a.vr] = a.sign
                is LitAnd -> for (lit in a.asIterable) {
                    mm[lit.vr] = lit.sign
                }
            }
            return mm
        }
    }

    var conflictLit: Lit? = null

    constructor(assignment: Assignment?) : this(buildMap(assignment))

    fun assign(a: Assignment): LitAndBuilder {
        if (isFailed()) throw IllegalStateException()
        return when (a) {
            is Lit -> assignLit(a)
            is LitAnd -> assignLits(a.asIterable)
            else -> throw IllegalStateException()
        }
    }

    fun assignLits(lits: Iterable<Lit>): LitAndBuilder {
        if (isFailed()) throw IllegalStateException()
        for (lit in lits) {
            assignLit(lit)
            if (isFailed()) break
        }
        return this
    }

    private fun assignLit(lit: Lit): LitAndBuilder {
        if (isFailed()) throw IllegalStateException()
        val v = lit.vr
        val newValue = lit.sign
        val currentValue: Boolean? = map[v]
        return when (currentValue) {
            null -> {
                map[v] = newValue
                this
            }
            newValue -> this
            else -> {
                conflictLit = lit
                this
            }
        }
    }

    fun isFailed(): Boolean = conflictLit != null

    fun first(): Lit = map.first2()

    fun addLitUnsafe(lit: Lit) {
        put(lit.vr, lit.sign)
    }

    fun addLitAndUnsafe(lits: LitAnd) {
        for (lit in lits.asIterable) {
            addLitUnsafe(lit)
        }
    }

    fun addUnsafe(a: Assignment) {
        when (a) {
            is Lit -> addLitUnsafe(a)
            is LitAnd -> addLitAndUnsafe(a)
            else -> throw IllegalStateException()
        }
    }

    fun mkAssignment(): Assignment {
        check(!isEmpty())
        return mk() as Assignment
    }

    fun mk(): Exp {
        return when (size) {
            0 -> True()
            1 -> first()
            else -> LitAnd(map)
        }
    }

    fun mk(f: VarSpace): Exp {
        return f.mk(mk())
    }

    override fun toString(): String {
        return asIterable().toList().toString()
    }
}