package org.smartsoft.konfigurator

/**
 * May represent a single lit or a set of lits
 */

interface Assignment {
    fun value(v: Var): Tri
    fun containsVar(v: Var): Boolean
    fun isEmpty(): Boolean
    fun isNotEmpty() = !isEmpty()
    val vars: Set<Var>
    val lits1: Iterable<Lit>
    fun anyOverlap(a: Assignment): Boolean

    fun asExp(): Exp = when {
        this is Lit -> this
        this is LitAnd -> this
        else -> throw IllegalStateException()
    }


}