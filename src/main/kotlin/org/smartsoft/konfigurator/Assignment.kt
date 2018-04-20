package org.smartsoft.konfigurator

/**
 * May represent a single lit or a set of lits
 */

interface Assignment {

    fun value(v: Var): Tri

    fun containsVar(v: Var): Boolean

    fun isEmpty(): Boolean

    val asIterable: Iterable<Lit>

    val asExp: Exp
        get() = when {
            this is Lit -> this
            this is LitAnd -> this
            else -> throw IllegalStateException()
        }

}