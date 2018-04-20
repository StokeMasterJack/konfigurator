@file:Suppress("REDUNDANT_MODIFIER")

package org.smartsoft.konfigurator

import kotlin.reflect.KClass


/*
Exp
    Simple
        Constant
            True
            False
        Lit
            Var
            NegVar
    Complex
        And
            LitAnd
            ComplexAnd
            MixedAnd
        NonAnd
            Not
            Conflict
            Requires
            Iff
            Or
            Xor
 */

typealias VarId = String

sealed class Exp : Comparable<Exp> {

    abstract val vars: Set<Var>

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        return when (this) {
            is Simple -> this === other
            is Not -> other is Not && eq(other)
            is Iff -> other is Iff && eq(other)
            is Conflict -> other is Iff && eq(other)
            is Requires -> other is Requires && eq(other)
            is LitAnd -> other is LitAnd && eq(other)
            is MixedAnd -> other is MixedAnd && eq(other)
            is ComplexAnd -> other is ComplexAnd && eq(other)
            is Xor -> other is Xor && eq(other)
            is Or -> other is Or && eq(other)
        }
    }

    override fun hashCode(): Int {
        if (this is Simple) {
            return System.identityHashCode(this)
        } else {
            throw UnsupportedOperationException()
        }
    }

    fun isDisjointDeep(): Boolean {
        return when (this) {
            is Simple -> true
            is Complex -> {
                if (this is And) {
                    val local = this.computeLocalDisjoint()
                    if (!local) return false
                }
                for (exp in exps) {
                    if (!exp.isDisjointDeep()) return false
                }
                return true
            }
        }

    }

    fun isDisjoint(other: Exp) = !anyOverlap(other)

    fun isDisjoint(a: Assignment) = !anyOverlap(a.asExp)

    fun anyOverlap(a: Assignment): Boolean {
        return anyOverlap(a.asExp)
    }

    fun anyOverlap(other: Exp): Boolean = when (this) {
        is Constant -> false
        is Lit -> when (other) {
            is Constant -> false
            is Lit -> other.vr === vr
            else -> other.containsVar(vr)
        }
        else -> when (other) {
            is Constant -> false
            is Lit -> containsVar(other.vr)
            else -> vars.anyOverlap(other.vars)
        }
    }

    fun containsVar(v: Var): Boolean = when (this) {
        is Constant -> false
        is Lit -> vr == v
        is Binary -> e1.containsVar(v) || e2.containsVar(v)
        is LitAnd -> vars.contains(v)
        is MixedAnd -> constraint.containsVar(v) || lits.containsVar(v)
        is ComplexAnd -> exps.any { it.containsVar(v) }
        is Not -> exp.containsVar(v)
        is NonAnd -> exps.any { it.containsVar(v) }
    }

    abstract fun assign(pics: Assignment, f: ExpFactory): Exp

    abstract fun maybeSimplify(f: ExpFactory): Exp

    fun maybeSimplify(a: Assignment, f: ExpFactory): Exp {
        if (!anyOverlap(a)) return this
        val after = simplify(a, f)
        check(after !== this)
        return after
    }

    abstract fun simplify(assignment: Assignment, f: ExpFactory): Exp


    val typeDetail: KClass<out Exp>
        get() = this::class

    val type: KClass<out Exp>
        get() = when (this) {
            is And -> And::class
            else -> this::class
        }

    open val op: String
        get() = type.simpleName!!


    open val opDetail: String
        get() = typeDetail.simpleName!!

    final override fun toString() = toString(false)

    fun toStringDetail() = toString(true)

    fun toString(detail: Boolean): String {
        val op = if (detail) this.opDetail else this.op
        return when (this) {
            is Constant -> op
            is Var -> id
            is NegVar -> "!${vr.id}"
            is Requires -> "$op$exps"
            is Complex -> "$op${exps.sorted()}"
        }
    }

    fun flip(f: ExpFactory): Exp = when (this) {
        is False -> f.mkTrue()
        is True -> f.mkFalse()
        is Var -> neg
        is NegVar -> vr
        is Not -> exp
        is Complex -> f.mkNot(this)
    }

    internal fun compareIndex1(): Int = when (this) {
        is Constant -> 1
        is Lit -> 2
        is Complex -> 3
    }

    internal fun compareIndex2(): Int = when (this) {
        is Constant -> when (this) {
            is True -> 1
            is False -> 2
        }
        is Lit -> when (this) {
            is Var -> 1
            is NegVar -> 2
        }
        is Complex -> when (this) {
            is Not -> 2
            else -> 1
        }
    }

    override fun compareTo(other: Exp): Int {
        val i1 = this.compareIndex1().compareTo(other.compareIndex1())
        if (i1 != 0) return i1
        val i2 = this.compareIndex2().compareTo(other.compareIndex2())
        if (i2 != 0) return i2

        return when (this) {
            is Constant -> throw IllegalStateException()
            is Lit -> this.compareLit(other as Lit)
            is Complex -> this.compareComplex(other as Complex)
        }
    }

} //end Exp


sealed class Simple : Exp()

sealed class Constant : Simple() {

    override val vars: Set<Var> by lazy {
        emptySet<Var>()
    }

    override fun maybeSimplify(f: ExpFactory): Exp {
        return this
    }

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        throw UnsupportedOperationException()
    }

}

class True : Constant() {
    override fun assign(pics: Assignment, f: ExpFactory): Exp = pics.asExp
}

class False : Constant() {
    override fun assign(pics: Assignment, f: ExpFactory): Exp = f.mkFalse()
}

sealed class Complex : Exp() {

    open val exps: Iterable<Exp>
        get() = when (this) {
            is Not -> listOf(exp)
            is Binary -> listOf(e1, e2)
            is Nary -> throw UnsupportedOperationException("Overridden")
            else -> throw IllegalStateException()
        }

    val size: Int
        get() = when (this) {
            is LitAnd -> map.size
            is Not -> 1
            is Binary -> 2
            is Or -> exps.size
            is Xor -> exps.size
            is ComplexAnd -> exps.size
            else -> throw IllegalStateException()
        }


    fun isEmpty(): Boolean = size == 0
    fun isNotEmpty(): Boolean = !isEmpty()

    fun compareComplex(other: Complex): Int {
        val i1 = opDetail.compareTo(other.opDetail)
        if (i1 != 0) return i1
        val i2 = size.compareTo(other.size)
        if (i2 != 0) return i2
        val expList1 = this.exps.sorted()
        val expList2 = other.exps.sorted()
        for (index in 0 until size) {
            val e1 = expList1[index]
            val e2 = expList2[index]
            val i3 = e1.compareTo(e2)
            if (i3 != 0) return i3
        }
        return 0
    }


}

interface Binary {
    val e1: Exp
    val e2: Exp
}

interface Nary

class Not(val exp: Complex) : NonAnd() {

    init {
        check(exp !is Not)
    }

    override fun maybeSimplify(f: ExpFactory): Exp {
        val s = exp.maybeSimplify(f)
        if (s == exp) return this
        return s.flip(f)
    }

    override val vars: Set<Var> get() = exp.vars

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        val ss = exp.simplify(assignment, f)
        return ss.flip(f)
    }

    fun eq(other: Not) = exp == other.exp

}

sealed class Lit : Simple(), Assignment {

    abstract val vr: Var

    override val vars: Set<Var> by lazy { setOf(vr) }

    override fun maybeSimplify(f: ExpFactory): Exp = this

    override fun isEmpty(): Boolean = false

    override fun value(v: Var): Tri {
        return when {
            v !== this.vr -> Tri.OPEN
            this.sign -> Tri.TRUE
            else -> Tri.FALSE
        }
    }

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        val boolValue = assignment.value(vr).toBool()
        return if (boolValue == sign) {
            f.mkTrue()
        } else {
            f.mkFalse()
        }
    }

    val sign: Boolean
        get() = when (this) {
            is Var -> true
            is NegVar -> false
        }

    operator fun not(): Lit = when (this) {
        is Var -> this.neg
        is NegVar -> this.vr
    }

    override fun assign(pics: Assignment, f: ExpFactory): Exp {
        return if (pics is Lit) {
            if (pics.vr == vr) {
                if (pics.sign == sign) this
                else f.mkFalse()
            } else {
                f.mkAndDisjoint(this as Assignment, pics as Assignment)
            }
        } else {
            if (pics.containsVar(vr)) {
                f.mkAnd(this as Assignment, pics)
            } else {
                f.mkAndDisjoint(this as Assignment, pics)
            }
        }
    }

    override val asIterable: Iterable<Lit> by lazy {
        this.mkSingletonIterable()
    }

    internal fun compareLit(other: Lit): Int {
        val i = vr.id.compareTo(other.vr.id)
        return if (i != 0) {
            i
        } else {
            sign.compareTo(other.sign)
        }
    }

}


class Var(val id: VarId) : Lit() {

    override val vr: Var get() = this

    val neg: NegVar by lazy { NegVar(this) }

    fun lit(sign: Boolean): Lit {
        return if (sign) this else neg
    }


}


class NegVar(val _vr: Var) : Lit() {

    override val vr: Var get() = _vr


}


class Conflict(override val e1: Exp, override val e2: Exp) : NonAnd(), Binary {

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        val s1 = e1.maybeSimplify(assignment, f)
        val s2 = e2.maybeSimplify(assignment, f)
        if (s1 == f.mkTrue() && s2 == f.mkTrue()) return f.mkFalse()
        if (s1 == f.mkFalse() || s2 == f.mkFalse()) return f.mkTrue()
        if (s1 == f.mkTrue()) return s2.flip(f)
        if (s2 == f.mkTrue()) return s1.flip(f)

        if (s1 == e1 && s2 == e2) return this

        return f.mkConflict(s1, s2)
    }

    fun eq(other: Iff) = e1 == other.e1 && e2 == other.e2 || e1 == other.e2 && e2 == other.e1
}


class Iff(override val e1: Exp, override val e2: Exp) : NonAnd(), Binary {

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        val s1 = e1.maybeSimplify(assignment, f)
        val s2 = e2.maybeSimplify(assignment, f)
        if (s1 === f.mkTrue() && s2 == f.mkTrue()) return f.mkTrue()
        if (s1 === f.mkFalse() && s2 == f.mkFalse()) return f.mkTrue()
        if (s1 === f.mkFalse() && s2 == f.mkTrue()) return f.mkFalse()
        if (s1 === f.mkTrue() && s2 == f.mkFalse()) return f.mkFalse()
        if (s1 === f.mkTrue()) return s2
        if (s1 === f.mkFalse()) return s2.flip(f)
        if (s2 === f.mkTrue()) return s1
        if (s2 === f.mkFalse()) return s1.flip(f)
        if (s1 === e1 && s2 == e2) return this

        return f.mkIff(s1, s2)
    }

    fun eq(other: Iff) = e1 == other.e1 && e2 == other.e2 || e1 == other.e2 && e2 == other.e1

}


class Requires(override val e1: Exp, override val e2: Exp) : NonAnd(), Binary {

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        val s1 = e1.maybeSimplify(assignment, f)
        val s2 = e2.maybeSimplify(assignment, f)
        if (s1 === f.mkTrue() && s2 == f.mkFalse()) return f.mkFalse()
        if (s1 === f.mkFalse()) return f.mkTrue()
        if (s2 === f.mkTrue()) return f.mkTrue()
        if (s1 === f.mkTrue()) return s2
        if (s2 === f.mkFalse()) return s1.flip(f)

        if (s1 == e1 && s2 == e2) return this

        return f.mkRequire(s1, s2)
    }

    fun eq(other: Requires) = e1 == other.e1 && e2 == other.e2
}


class Xor(override val exps: List<Exp>) : NonAnd(), Nary {

    override val vars: Set<Var>
        get() = exps.vars()

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        require(this.anyOverlap(assignment))

        val simple = exps.map { it.maybeSimplify(assignment, f) }

        val trueCount = simple.count { it === f.mkTrue() }
        val opens = simple.filter { it !is Constant }

        return when {
            trueCount == 0 -> when {
                opens.isEmpty() -> f.mkFalse()
                opens.size == 1 -> opens[0]
                else -> f.mkXor(opens)
            }
            trueCount == 1 -> when {
                opens.isEmpty() -> f.mkTrue()
                opens.size == 1 -> opens[0].flip(f)
                else -> {
                    val flipped = opens.map { it.flip(f) }
                    f.mkAnd(flipped)
                }
            }
            trueCount > 1 -> f.mkFalse()
            else -> throw IllegalStateException()
        }
    }

    fun eq(other: Xor) = exps.sorted() == other.exps.sorted()

}

abstract sealed class And : Complex() {
    abstract fun computeLocalDisjoint(): Boolean

}

abstract sealed class NonAnd : Complex() {


    override fun assign(pics: Assignment, f: ExpFactory): Exp {
        return Propagator.propagate(f, this, pics)
    }

    override fun maybeSimplify(f: ExpFactory): Exp = this

    override val vars: Set<Var> by lazy { exps.vars() }

}


class Or(override val exps: List<Exp>) : NonAnd(), Nary {

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        val ss = mutableListOf<Exp>()
        for (e in exps) {
            check(e !is Constant)
            val s = e.maybeSimplify(assignment, f)
            if (s == f.mkFalse()) continue
            else if (s == f.mkTrue()) return f.mkTrue()
            else if (s is Or) ss.addAll(s.exps)
            else ss.add(s)
        }
        return f.mkOr(ss)
    }

    override fun maybeSimplify(f: ExpFactory): Exp = when (exps.size) {
        0 -> f.mkFalse()
        1 -> exps[0]
        else -> this
    }

    fun eq(other: Or) = exps.sorted() == other.exps.sorted()
}

enum class AssignAffect {
    DUP, CONFLICT, ASSIGN
}


class LitAnd(val map: Map<Var, Boolean>) : And(), Assignment, Nary {

    constructor(lit: Lit) : this(mapOf(lit.vr to lit.sign))

    constructor(lit1: Lit, lit2: Lit) : this(mapOf(lit1.vr to lit1.sign, lit2.vr to lit2.sign)) {
        require(lit1.vr != lit2.vr)
    }

    override fun computeLocalDisjoint(): Boolean {
        return true
    }

    override val exps: Iterable<Exp> get() = asIterable

    override val asIterable: Iterable<Lit> by lazy {
        object : Iterable<Lit> {
            override fun iterator() = LitIt(map.iterator())
        }
    }

    override val vars: Set<Var> by lazy { map.keys }

    val tLits: List<Lit> get() = asIterable.filter { it.sign }

    fun copy(): LitAndBuilder {
        val m = LitAndBuilder()
        m.putAll(map)
        return m
    }

    override fun assign(pics: Assignment, f: ExpFactory): Exp {
        val affect = assignTest(pics)
        return when (affect) {
            AssignAffect.DUP -> this
            AssignAffect.CONFLICT -> f.mkFalse()
            AssignAffect.ASSIGN -> {
                val mm = copy()
                mm.assign(pics)
                when {
                    mm.isFailed() -> throw IllegalStateException()
                    mm.isEmpty() -> f.mkTrue()
                    mm.size == 1 -> mm.first()
                    else -> f.mk(LitAnd(mm))
                }
            }
        }
    }

    private fun assignTest(a: Assignment): AssignAffect = when (a) {
        is Lit -> assignLitTest(a)
        is LitAnd -> assignLitAndTest(a)
        else -> throw IllegalStateException()
    }

    private fun assignLitTest(lit: Lit): AssignAffect {
        val b = map[lit.vr]
        return when (b) {
            null -> AssignAffect.ASSIGN
            lit.sign -> AssignAffect.DUP
            else -> AssignAffect.CONFLICT
        }
    }

    private fun assignLitsTest(lits: Iterable<Lit>): AssignAffect {
        var anyChange = false
        for (lit in lits) {
            val affect = assignLitTest(lit)
            when (affect) {
                AssignAffect.CONFLICT -> return AssignAffect.CONFLICT
                AssignAffect.ASSIGN -> anyChange = true
                AssignAffect.DUP -> Unit //ignore
            }
        }
        return if (anyChange) AssignAffect.ASSIGN
        else AssignAffect.DUP
    }

    private fun assignLitAndTest(litAnd: LitAnd): AssignAffect {
        return assignLitsTest(litAnd.asIterable)
    }

    override fun maybeSimplify(f: ExpFactory): Exp = when (size) {
        0 -> f.mkTrue()
        1 -> first()
        else -> this
    }

    private fun first(): Lit = map.first1()

    fun eq(other: LitAnd) = map == other.map

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        val mm = LitAndBuilder()
        for (lit in asIterable) {
            val s = lit.maybeSimplify(assignment, f)
            if (s is True) continue
            if (s is False) return f.mkFalse()
            mm.addLitUnsafe(lit)
        }
        return when {
            mm.isEmpty() -> f.mkTrue()
            mm.size == 1 -> mm.first()
            else -> LitAnd(mm)
        }
    }

    companion object {
        val EMPTY: LitAnd = LitAnd(emptyMap())
        fun create(lit: Lit): LitAnd = LitAnd(lit)

    }

    fun minus(vr: Var): LitAnd = LitAnd(map.minus(vr))

    override fun value(v: Var): Tri = Tri.fromBoolean(map[v])

}

class ComplexAnd(override val exps: List<NonAnd>) : And(), Nary {

    override val vars: Set<Var> by lazy { exps.vars() }

    override fun computeLocalDisjoint(): Boolean {
        val set = mutableSetOf<Var>()
        for (exp in exps) {
            val dd = set.isDisjoint(exp)
            if (!dd) return false
            set.addAll(exp.vars)
        }
        return true
    }

    fun first(): Exp = exps[0]

    override fun maybeSimplify(f: ExpFactory): Exp = when (size) {
        0 -> f.mkTrue()
        1 -> first()
        else -> this
    }

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        return f.mkAnd(exps, assignment)
    }

    override fun assign(pics: Assignment, f: ExpFactory): Exp {
        return Propagator.propagate(f, this, pics)
    }

    fun eq(other: ComplexAnd) = exps.sorted() == other.exps.sorted()


}


class MixedAnd(val constraint: Complex, val lits: Assignment, val disjoint: Boolean) : And(), Binary {

    init {
        require(!lits.isEmpty())
        require(constraint !is LitAnd)
        if (disjoint) {
            require(constraint.isDisjoint(lits))
        }
    }

    override val e1: Exp get() = constraint

    override val e2: Exp get() = lits.asExp

    override fun computeLocalDisjoint(): Boolean = constraint.isDisjoint(lits)

    override fun assign(pics: Assignment, f: ExpFactory): Exp {
        return Propagator.propagate(f, this, pics)
    }

    override fun maybeSimplify(f: ExpFactory): Exp = if (disjoint) {
        this
    } else {
        Propagator.propagate(f, constraint, lits)
    }

    fun overlapLits(): Assignment {
        return if (disjoint) {
            LitAnd.EMPTY
        } else {
            val mm = LitAndBuilder()
            for (lit in lits.asIterable) {
                if (constraint.containsVar(lit.vr)) {
                    mm.addUnsafe(lit)
                }
            }
            mm.mkAssignment()
        }
    }

    override val exps: Iterable<Exp>
        get() = listOf(constraint, lits.asExp)

    override fun simplify(assignment: Assignment, f: ExpFactory): Exp {
        throw UnsupportedOperationException()
    }

    override val vars: Set<Var>
        get() = when (lits) {
            is LitAnd -> constraint.vars + lits.vars
            is Lit -> constraint.vars + lits.vr
            else -> throw IllegalStateException()
        }

    fun eq(other: MixedAnd) = constraint == other.constraint && lits == other.lits

}


//extension functions

fun Iterable<Exp>.vars(): Set<Var> = flatMap { it.vars }.toSet()

fun Set<Var>.anyOverlap(vars: Set<Var>): Boolean = this.any { vars.contains(it) }
fun Set<Var>.isDisjoint(vars: Set<Var>): Boolean = !anyOverlap(vars)

fun Set<Var>.anyOverlap(exp: Exp): Boolean = anyOverlap(exp.vars)
fun Set<Var>.isDisjoint(exp: Exp): Boolean = isDisjoint(exp.vars)

fun Map.Entry<Var, Boolean>.toLit1(): Lit = this.key.lit(value)
fun Map<Var, Boolean>.first1(): Lit = this.iterator().next().toLit1()

fun MutableMap.MutableEntry<Var, Boolean>.toLit2(): Lit = key.lit(value)
fun MutableMap<Var, Boolean>.first2(): Lit = this.iterator().next().toLit2()

