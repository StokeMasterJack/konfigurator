package org.smartsoft.konfigurator

import kotlin.reflect.KClass


/*
Exp
    Simple
        Constant
        Lit
    Complex
        And
            LitAnd
            ComplexAnd
            MixedAnd
        NonAnd
            Not
            Pair
                Conflict
                Requires
                Iff
            Or
 */

typealias VarId = String

object True : Constant() {
    override fun assign(pics: Assignment): Exp = pics.asExp()
}

object False : Constant() {
    override fun assign(pics: Assignment): Exp = False
}

sealed class Exp : Comparable<Exp> {

    abstract val expFactory: ExpFactory

    val f: ExpFactory get() = expFactory

    abstract val exps: Iterable<Exp>

    open val e1: Exp get() = throw UnsupportedOperationException(this.opDetail)
    open val e2: Exp get() = throw UnsupportedOperationException(this.opDetail)

    val expList: List<Exp> get() = exps.toList()

    /**
     * Number of child expressions
     */
    abstract val size: Int

    companion object {

        fun ensureNoConstants(exps: Iterable<Exp>) {
            check(exps.all { it !is Constant }, { exps })
        }

        fun ensureNoLits(exps: Iterable<Exp>) {
            check(exps.all { it !is Lit }, { exps })
        }

        fun ensureNoAnds(exps: Iterable<Exp>) {
            check(exps.all { it !is And }, { exps })
        }

        fun ensureNoConstants(e1: Exp, e2: Exp) {
            check(e1 !is Constant)
            check(e2 !is Constant)
        }

        fun computeVars(exps: Iterable<Exp>): Set<Var> {
            return exps.flatMap { it.vars }.toSet()
        }

        fun eq(e1: LitAnd, e2: LitAnd): Boolean {
            return e1.map == e2.map
        }

        fun eq(e1: Complex, e2: Complex): Boolean {
            if (e1.size != e2.size) return false
            val exps1 = e1.exps.sorted()
            val exps2 = e2.exps.sorted()
            for (i in 0 until e1.size) {
                if (exps1[i] != exps2[i]) return false
            }
            return true
        }

        fun eqReq(ths: Requires, other: Requires): Boolean {
            if (ths.e1 != other.e1) return false
            if (ths.e2 != other.e2) return false
            return true
        }


    }


    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this::class != other::class) return false
        if (this === other) return true
        return when (this) {
            is Simple -> this === other
            is LitAnd -> eq(this, other as LitAnd)
            is Requires -> eqReq(this, other as Requires)
            is Complex -> eq(this, other as Complex)
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

    abstract val vars: Set<Var>

    fun containsAllVars(a: Assignment) = when (this) {
        is Constant -> {
            false
        }
        is Lit -> when (a) {
            is Lit -> a.vr === vr
            is LitAnd -> a.containsVar(vr)
            else -> throw IllegalStateException()
        }
        else -> when (a) {
            is Lit -> containsVar(a.vr)
            is LitAnd -> vars.containsAll(a.vars)
            else -> throw IllegalStateException()
        }

    }

    fun isDisjoint(other: Exp) = !anyOverlap(other)
    fun isDisjoint(a: Assignment) = !anyOverlap(a)

    fun anyOverlap(a: Assignment): Boolean = when (a) {
        is Exp -> anyOverlap(a as Exp)
        else -> throw IllegalStateException()
    }

    fun anyOverlap(other: Exp): Boolean = when (this) {
        is Constant -> false
        is Lit -> when (other) {
            is Constant -> false
            is Lit -> other.vr == vr
            else -> other.containsVar(vr)
        }
        else -> when (other) {
            is Constant -> false
            is Lit -> containsVar(other.vr)
            else -> vars.anyOverlap(other.vars)
        }
    }

    fun anyOverlap(vars: Set<Var>): Boolean = when (this) {
        is Constant -> false
        is Lit -> vars.contains(vr)
        is Pair -> e1.anyOverlap(vars) || e2.anyOverlap(vars)
        else -> exps.any { it.anyOverlap(vars) }
    }

    fun containsVar(v: Var): Boolean = when (this) {
        is Constant -> false
        is Lit -> vr == v
        is Pair -> e1.containsVar(v) || e2.containsVar(v)
        is LitAnd -> vars.contains(v)
        is MixedAnd -> constraint.containsVar(v) || lits.containsVar(v)
        is ComplexAnd -> exps.any { it.containsVar(v) }
        is Not -> exp.containsVar(v)
        is NonAndComplex -> exps.any { it.containsVar(v) }
    }

    abstract fun assign(pics: Assignment): Exp

    abstract fun maybeSimplify(): Exp

    fun maybeSimplify(lits: Assignment): Exp {
        if (!anyOverlap(lits)) return this
        val after = simplify(lits)
        check(after !== this)
        return after
    }

    fun maybeSimplify(lits: List<Lit>): Exp {
        val aa = MutableLitAnd()
        aa.assignLitsInPlace(lits)
        return maybeSimplify(aa)
    }

    fun maybeSimplify(vararg lits: Lit): Exp {
        val aa = MutableLitAnd()
        for (lit in lits) aa.assignLitInPlace(lit)
        return maybeSimplify(aa)
    }

    abstract fun simplify(lits: Assignment): Exp

    val typeDetail: KClass<out Exp>
        get() = when (this) {
            is ComplexAnd -> ComplexAnd::class
            is LitAnd -> LitAnd::class
            is MixedAnd -> MixedAnd::class
            else -> this::class
        }

    val type: KClass<out Exp>
        get() = when (this) {
            is And -> And::class
            else -> this::class
        }

    open val op: String get() = type.simpleName!!
    open val opDetail: String get() = typeDetail.simpleName!!

    open val lits: LitAnd get() = throw UnsupportedOperationException()

    final override fun toString(): String {
        return when (this) {
            is Constant -> op
            is Var -> id
            is NegVar -> "!${vr.id}"
            is Requires -> "$op${exps}"
            is Complex -> "$op${exps.sorted()}"
        }
    }

    fun toStringDetail(): String {
        return when (this) {
            is Constant -> opDetail
            is Var -> id
            is NegVar -> "!${vr.id}"
            is Complex -> "$opDetail${exps.sorted()}"
        }
    }

    fun flip(): Exp = when (this) {
        False -> True
        True -> False
        is Var -> neg
        is NegVar -> vr
        is Not -> exp
        is Complex -> expFactory.mkNot(this)
    }

    private fun compareIndex1(): Int = when (this) {
        is Constant -> 1
        is Lit -> 2
        is Complex -> 3
    }

    private fun compareIndex2(): Int = when (this) {
        is Constant -> if (this === False) 1 else 2
        is Lit -> 1
        is Complex -> size
    }

    private fun compareLit(other: Lit) = if (this is Lit) {
        val i = vr.id.compareTo(other.vr.id)
        if (i != 0) {
            i
        } else {
            sign.compareTo(other.sign)
        }
    } else {
        throw IllegalStateException()
    }


    private fun compareExps(other: Complex): Int {
        val i1 = this.size.compareTo(other.size)
        if (i1 != 0) return i1
        if (this is Complex) {
            val it = other.exps.iterator()
            for (exp in exps) {
                val expOther = it.next()
                val i2 = exp.compareTo(expOther)
                if (i2 != 0) return i2
            }
            return 0
        } else {
            throw IllegalStateException()
        }
    }

    override fun compareTo(other: Exp): Int {
        val i1 = compareIndex1().compareTo(other.compareIndex1())
        if (i1 != 0) return i1
        val i2 = compareIndex2().compareTo(other.compareIndex2())
        if (i2 != 0) return i2
        return when (this) {
            is Constant -> throw IllegalStateException()
            is Lit -> compareLit(other as Lit)
            is Complex -> compareExps(other as Complex)
        }
    }
}

fun Set<Var>.anyOverlap(vars: Set<Var>): Boolean {
    return this.any {
        val vv = it
        vars.contains(vv)
    }
}

fun Set<Var>.isDisjoint(vars: Set<Var>) = !anyOverlap(vars)
fun Set<Var>.isDisjoint(exp: Exp) = isDisjoint(exp.vars)

sealed class Simple : Exp() {
    override val exps: Iterable<Exp> get() = throw UnsupportedOperationException()
    override val e1: Exp get() = throw UnsupportedOperationException()
    final override val size: Int get() = 0
}

sealed class Complex : Exp() {
    override val expFactory: ExpFactory
        get() = e1.expFactory
}


sealed class Pair(final override val e1: Exp, final override val e2: Exp) : NonAndComplex() {

    init {
        ensureNoConstants(e1, e2)
    }

    override val exps: List<Exp> get() = listOf(e1, e2)

    override val size: Int get() = 2

    override val vars: Set<Var>
        get() {
            val s = mutableSetOf<Var>()
            s.addAll(e1.vars)
            s.addAll(e2.vars)
            return s
        }
}

class Not(val exp: Complex) : NonAndComplex() {

    init {
        require(exp !is Constant)
        check(exp !is Not)
    }

    override val size: Int get() = 1
    override val exps: List<Exp> get() = listOf(exp)
    override val e1: Exp get() = exp

    override fun maybeSimplify(): Exp {
        val s = exp.maybeSimplify()
        if (s == exp) return this
        return s.flip()
    }

    override val vars: Set<Var>
        get() = exp.vars

    override fun simplify(lits: Assignment): Exp {
        val ss = exp.simplify(lits)
        return ss.flip()
    }

}

sealed class Lit : Simple(), Assignment {

    abstract val vr: Var

    val asLitAnd: LitAnd by lazy {
        LitAnd.create(this)
    }

    override val lits: LitAnd
        get() = asLitAnd

    override val lits1: Iterable<Lit> by lazy {
        object : Iterable<Lit> {
            override fun iterator() = SingleLitIt(this@Lit)
        }
    }

    override fun maybeSimplify(): Exp = this

    private class SingleLitIt(private val _lit: Lit) : Iterator<Lit> {

        private var hasNext: Boolean = true

        override fun hasNext(): Boolean {
            return hasNext
        }

        override fun next(): Lit {
            val retVal = _lit
            hasNext = false
            return retVal
        }
    }

    override fun isEmpty(): Boolean = false

    override fun value(v: Var): Tri {
        return when {
            v !== this.vr -> Tri.OPEN
            this.sign -> Tri.TRUE
            else -> Tri.FALSE
        }
    }

    val sign: Boolean
        get() = when (this) {
            is Var -> true
            is NegVar -> false
        }

    operator fun not(): Lit {
        return when {
            this is Var -> this.neg
            this is NegVar -> this.vr
            else -> throw IllegalStateException()
        }
    }

    override fun assign(pics: Assignment): Exp = when (pics) {
        is Lit -> assignLit(pics)
        is LitAnd -> assignLitAnd(pics)
        else -> throw IllegalStateException()
    }

    open fun assignLit(pic: Lit): Exp {
        return if (vr !== pic.vr) {
            MutableLitAnd().apply {
                addUnsafe(this@Lit)
                addUnsafe(pic)
            }
        } else {
            if (sign == pic.sign) {
                check(this === pic)
                this
            } else {
                False
            }
        }
    }

    open fun assignLitAnd(pics: LitAnd): Exp {
        return pics.assignLit(this)
    }


}

sealed class Constant : Simple() {

    override val vars: Set<Var> by lazy {
        emptySet<Var>()
    }

    override fun maybeSimplify(): Exp {
        return this
    }

    override fun simplify(lits: Assignment): Exp {
        throw UnsupportedOperationException()
    }

    override val expFactory: ExpFactory
        get() = throw UnsupportedOperationException(this.toString())

}


class Var(private val _expFactory: ExpFactory, val id: VarId) : Lit() {

    override val expFactory: ExpFactory get() = _expFactory

    val neg: NegVar by lazy {
        NegVar(this)
    }

    override val vars: Set<Var> by lazy {
        setOf(this)
    }

    override val vr: Var get() = this

    fun lit(sign: Boolean): Lit {
        return if (sign) this else neg
    }

    override fun simplify(lits: Assignment) = when (lits.value(this)) {
        Tri.TRUE -> True
        Tri.FALSE -> False
        Tri.OPEN -> throw IllegalStateException()
    }

}

class NegVar(override val vr: Var) : Lit() {

    override val expFactory: ExpFactory
        get() = vr.expFactory

    override fun simplify(lits: Assignment) = when (lits.value(this.vr)) {
        Tri.TRUE -> False
        Tri.FALSE -> True
        Tri.OPEN -> throw IllegalStateException()
    }

    override val vars: Set<Var> = vr.vars

}

class Conflict(e1: Exp, e2: Exp) : Pair(e1, e2) {
    override fun simplify(lits: Assignment): Exp {
        val s1 = e1.maybeSimplify(lits)
        val s2 = e2.maybeSimplify(lits)
        if (s1 == True && s2 == True) return False
        if (s1 == False || s2 == False) return True
        if (s1 == True) return s2.flip()
        if (s2 == True) return s1.flip()

        if (s1 == e1 && s2 == e2) return this

        return f.mkConflict(s1, s2)
    }
}

class Iff(e1: Exp, e2: Exp) : Pair(e1, e2) {
    override fun simplify(lits: Assignment): Exp {
        val s1 = e1.maybeSimplify(lits)
        val s2 = e2.maybeSimplify(lits)
        if (s1 == True && s2 == True) return True
        if (s1 == False && s2 == False) return True
        if (s1 == False && s2 == True) return False
        if (s1 == True && s2 == False) return False
        if (s1 == True) return s2
        if (s1 == False) return s2.flip()
        if (s2 == True) return s1
        if (s2 == False) return s1.flip()
        if (s1 == e1 && s2 == e2) return this
        return Iff(s1, s2)
    }
}

class Requires(e1: Exp, e2: Exp) : Pair(e1, e2) {

    override fun simplify(lits: Assignment): Exp {
        val s1 = e1.maybeSimplify(lits)
        val s2 = e2.maybeSimplify(lits)
        if (s1 === True && s2 == False) return False
        if (s1 === False) return True
        if (s2 === True) return True
        if (s1 === True) return s2
        if (s2 === False) return s1.flip()

        if (s1 == e1 && s2 == e2) return this

        return f.mkRequire(s1, s2)
    }
}

class Xor(override val exps: List<Exp>) : NonAndComplex() {

    init {
        require(exps.size > 1)
    }

    override val e1: Exp get() = exps[0]

    override val size: Int get() = exps.size


    override val vars: Set<Var> get() = Exp.computeVars(exps)

    override fun simplify(lits: Assignment): Exp {
        require(this.anyOverlap(lits))

        val simple = exps.map { it.maybeSimplify(lits) }

        val trueCount = simple.count { it === True }
        val opens = simple.filter { it !is Constant }

        return when {
            trueCount == 0 -> when {
                opens.isEmpty() -> False
                opens.size == 1 -> opens[0]
                else -> expFactory.mkXor(opens)
            }
            trueCount == 1 -> when {
                opens.isEmpty() -> True
                opens.size == 1 -> opens[0].flip()
                else -> {
                    val b = AndBuilder()
                    for (open in opens) {
                        b.add(open.flip())
                    }
                    expFactory.mkAnd(b)
                }
            }
            trueCount > 1 -> False
            else -> throw IllegalStateException()
        }
    }

}

sealed class And : Complex() {
    abstract fun computeLocalDisjoint(): Boolean
}

sealed class NonAndComplex : Complex() {

    override fun assign(pics: Assignment): Exp {
        return Propagator.propagate(f, this, pics)
    }

    override fun maybeSimplify(): Exp = this

}


class Or(override val exps: List<Exp>) : NonAndComplex() {

    override val size: Int get() = exps.size
    override val e1: Exp get() = exps[0]
    override val e2: Exp get() = exps[1]

    override val vars: Set<Var> by lazy {
        exps.flatMap { it.vars }.toSet()
    }

    override fun simplify(lits: Assignment): Exp {
        val ss = mutableListOf<Exp>()
        for (e in exps) {
            check(e !is Constant)
            val s = e.maybeSimplify(lits)
            if (s == False) continue
            else if (s == True) return True
            else if (s is Or) ss.addAll(s.exps)
            else ss.add(s)
        }
        return f.mkOr(ss)
    }

    override fun maybeSimplify(): Exp = when (exps.size) {
        0 -> False
        1 -> exps[0]
        else -> this
    }

}

enum class AssignAffect {
    DUP, CONFLICT, ASSIGN
}

abstract class LitAnd : And(), Assignment {

    override fun computeLocalDisjoint(): Boolean {
        return true
    }

    override val lits: LitAnd
        get() = this

    abstract override val vars: Set<Var>

    abstract override fun isEmpty(): Boolean

    abstract fun litIt(): Iterator<Lit>

    abstract val sorted: List<Lit>

    abstract val map: Map<Var, Boolean>

    abstract fun isFailed(): Boolean

    abstract fun computeOverlap(careVars: Set<Var>): MutableLitAnd
    abstract fun split(careVars: Set<Var>): DisjointLits

    abstract fun copy(): MutableLitAnd

    abstract fun assignLit(lit: Lit): Exp
    abstract fun assignLitAnd(lits: LitAnd): Exp
    abstract fun assignLits(lits: Iterable<Lit>): Exp

    abstract fun careLits(careVars: Set<Var>): LitAnd

    abstract fun toList(): List<Lit>
    fun toSet(): Set<Lit> = toList().toSet()

    fun assignTest(a: Assignment): AssignAffect = when (a) {
        is Lit -> assignLitTest(a)
        is LitAnd -> assignLitAndTest(a)
        else -> throw IllegalStateException()
    }

    fun assignLitTest(lit: Lit): AssignAffect {
        val b = map[lit.vr]
        return when (b) {
            null -> AssignAffect.ASSIGN
            lit.sign -> AssignAffect.DUP
            else -> AssignAffect.CONFLICT
        }
    }

    fun assignLitsTest(lits: Iterable<Lit>): AssignAffect {
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

    fun assignLitAndTest(litAnd: LitAnd): AssignAffect {
        return assignLitsTest(litAnd.lits1)
    }

    override fun assign(pics: Assignment): Exp {
        val affect = assignTest(pics)
        return when (affect) {
            AssignAffect.DUP -> this
            AssignAffect.CONFLICT -> False
            AssignAffect.ASSIGN -> copy().apply { assignInPlace(pics) }
        }
    }

    companion object {
        private val _empty = MutableLitAnd()

        fun empty() = _empty

        fun create(lit: Lit): LitAnd {
            return MutableLitAnd(lit)
        }


    }

    override fun maybeSimplify(): Exp = when {
        isFailed() -> False
        size == 0 -> True
        size == 1 -> e1
        else -> this
    }

    fun containsLit(lit: Lit): Boolean {
        val b = map[lit.vr]
        return b != null && b == lit.sign
    }

    fun containsLits(vararg lits: Lit): Boolean {
        for (lit in lits) {
            if (!containsLit(lit)) return false
        }
        return true
    }
}

fun Map.Entry<Var, Boolean>.toLit() = this.key.lit(value)

class MutableLitAnd(private val _map: MutableMap<Var, Boolean> = mutableMapOf()) : LitAnd() {

    override val sorted: List<Lit>
        get() = lits1.sorted()

    private var conflictLit: Lit? = null

    constructor(lit: Lit) : this() {
        _map[lit.vr] = lit.sign
    }

    constructor(lits: Iterable<Lit>) : this() {
        for (lit in lits) {
            assignLitInPlace(lit)
        }
    }

    override val map: Map<Var, Boolean> get() = _map

    override val opDetail: String
        get() = "LitAnd"

    override fun litIt(): Iterator<Lit> = LitIt(_map.iterator())

    override fun isEmpty(): Boolean {
        return _map.isEmpty()
    }

    override val vars: Set<Var>
        get() = _map.keys


    private class LitIt(private val mapIt: Iterator<Map.Entry<Var, Boolean>>) : Iterator<Lit> {

        override fun hasNext() = mapIt.hasNext()

        override fun next() = mapIt.next().toLit()
    }

    override val lits1: Iterable<Lit>
        get() = object : Iterable<Lit> {
            override fun iterator() = LitIt(_map.iterator())
        }

    override val exps: Iterable<Exp> get() = lits1

    override fun value(v: Var): Tri = Tri.fromBoolean(_map[v])

    override fun toList() = lits1.toList()

    override fun isFailed(): Boolean = conflictLit != null


    override fun assignLit(lit: Lit): Exp {
        val affect = assignLitTest(lit)
        return when (affect) {
            AssignAffect.DUP -> this
            AssignAffect.CONFLICT -> False
            AssignAffect.ASSIGN -> copy().apply { assignLitInPlace(lit) }
        }
    }

    override fun assignLitAnd(lits: LitAnd): Exp {
        if (lits.isFailed()) return False
        val affect = assignLitAndTest(lits)
        return when (affect) {
            AssignAffect.DUP -> this
            AssignAffect.CONFLICT -> False
            AssignAffect.ASSIGN -> copy().apply { assignLitAndInPlace(lits) }
        }
    }

    override fun assignLits(lits: Iterable<Lit>): Exp {
        val a = MutableLitAnd().apply { assignLitsInPlace(lits) }
        a.assignLitsInPlace(lits)
        if (a.isFailed()) return False
        return assignLitAnd(a)
    }

    fun assignInPlace(a: Assignment): AssignAffect = when (a) {
        is Lit -> assignLitInPlace(a)
        is LitAnd -> assignLitAndInPlace(a)
        else -> throw IllegalStateException()
    }

    fun assignLitAndInPlace(lits: LitAnd): AssignAffect {
        return assignLitsInPlace(lits.lits1)
    }

    fun assignLitsInPlace(lits: Iterable<Lit>): AssignAffect {
        if (isFailed()) throw IllegalStateException()
        var anyChange = false
        for (lit in lits) {
            val ch = assignLitInPlace(lit)
            if (ch == AssignAffect.CONFLICT) return AssignAffect.CONFLICT
            else if (ch == AssignAffect.ASSIGN) anyChange = true
        }
        return if (anyChange) {
            AssignAffect.ASSIGN
        } else {
            AssignAffect.DUP
        }
    }

    fun assignLitInPlace(lit: Lit): AssignAffect {
        if (isFailed()) throw IllegalStateException()
        val v = lit.vr
        val newValue = lit.sign
        val currentValue: Boolean? = _map[v]
        return when (currentValue) {
            null -> {
                _map[v] = newValue
                AssignAffect.ASSIGN
            }
            newValue -> AssignAffect.DUP
            else -> {
                conflictLit = lit
                AssignAffect.CONFLICT
            }
        }
    }


    override val size: Int
        get() = _map.size

    val firstLit: Lit get() = _map.entries.first().lit()

    override val e1: Exp get() = firstLit

    override fun copy(): MutableLitAnd {
        require(!isFailed())
        val c = MutableLitAnd()
        c._map.putAll(this._map)
        return c
    }

    override fun simplify(lits: Assignment) = expFactory.mkAnd(exps, lits)

    override fun computeOverlap(careVars: Set<Var>): MutableLitAnd {
        val filtered = MutableLitAnd()
        for (entry in _map.entries) {
            if (careVars.contains(entry.key)) {
                filtered._map[entry.key] = entry.value
            }
        }
        return filtered
    }

    override fun split(careVars: Set<Var>): DisjointLits {
        val overlap = MutableLitAnd()
        val nonOverlap = MutableLitAnd()
        for (entry in _map.entries) {
            if (careVars.contains(entry.key)) {
                overlap._map[entry.key] = entry.value
            } else {
                nonOverlap._map[entry.key] = entry.value
            }
        }

        return DisjointLits(overlap, nonOverlap)
    }


    override fun careLits(careVars: Set<Var>): LitAnd {
        if (isFailed()) throw IllegalStateException()
        val mm = MutableLitAnd()
        for (entry in _map.entries) {
            if (careVars.contains(entry.key)) {
                mm._map[entry.key] = entry.value
            }
        }
        return mm
    }

    fun addUnsafe(lit: Lit) {
        _map.put(lit.vr, lit.sign)
    }

    fun addAllUnsafe(lits: LitAnd) {
        for (lit in lits.lits1) {
            addUnsafe(lit)
        }
    }

    companion object {
        fun mkAndDisjoint(lits1: Assignment, lits2: Assignment): MutableLitAnd {
            require(!lits1.anyOverlap(lits2))
            val a = MutableLitAnd()
            when (lits1) {
                is LitAnd -> a._map.putAll(lits1.map)
                is Lit -> a._map.put(lits1.vr, lits1.sign)
                else -> throw IllegalStateException()
            }
            when (lits2) {
                is LitAnd -> a._map.putAll(lits2.map)
                is Lit -> a._map.put(lits2.vr, lits2.sign)
                else -> throw IllegalStateException()
            }
            return a
        }

    }

}

data class DisjointLits(val overlap: MutableLitAnd, val nonOverlap: MutableLitAnd) {

    init {
        check(overlap.isDisjoint(nonOverlap as Exp))
    }
}


fun Map.Entry<Var, Boolean>.lit(): Lit = key.lit(value)

/**
 * contains no constants, lits or ands
 */
abstract class ComplexAnd : And() {

    abstract override val exps: List<Complex>

    override fun computeLocalDisjoint(): Boolean {
        val set = mutableSetOf<Var>()
        for (exp in exps) {
            val dd = set.isDisjoint(exp)
            if (!dd) return false
            set.addAll(exp.vars)
        }
        return true
    }

    abstract fun isEmpty(): Boolean
    abstract fun isNotEmpty(): Boolean
    abstract fun first(): Exp

    override fun maybeSimplify(): Exp = when (size) {
        0 -> True
        1 -> e1
        else -> this
    }

    override val vars: Set<Var> by lazy {
        exps.flatMap { it.vars }.toSet()
    }

    override fun assign(pics: Assignment): Exp {
        if (pics is LitAnd && pics.isFailed()) return False
        return Propagator.propagate(f, this, pics)
    }


}

class MutableComplexAnd(override val exps: MutableList<Complex> = mutableListOf()) : ComplexAnd() {

    override val size: Int get() = exps.size

    override val e1: Exp get() = exps[0]
    override val e2: Exp get() = exps[1]

    init {
        ensureNoConstants(exps)
        ensureNoLits(exps)
        ensureNoAnds(exps)
    }


    override val opDetail: String
        get() = "ComplexAnd"

    override fun isEmpty(): Boolean = exps.isEmpty()
    override fun isNotEmpty(): Boolean = exps.isNotEmpty()
    override fun first(): Exp = exps.first()

    fun add(e: Complex) {
        check(e !is And)
        exps.add(e)
    }

    fun addAll(exps: Iterable<Complex>) {
        for (complex in exps) {
            add(complex)
        }
    }


    override fun simplify(lits: Assignment): Exp {
        return expFactory.mkAnd(exps, lits)
    }


}

class MixedAnd(val constraint: Complex, override val lits: LitAnd, val disjoint: Boolean) : And() {

    override val size: Int = 2

    override fun computeLocalDisjoint(): Boolean {
        val vars1 = constraint.vars
        val vars2 = lits.vars
        if (!vars1.isDisjoint(vars2)) return false
        return true
    }

    init {
        require(!lits.isFailed())
        require(lits.isNotEmpty())
        require(constraint is ComplexAnd || constraint is NonAndComplex, { constraint.toString() })
        if (disjoint) {
            require(lits.isDisjoint(constraint))
        }
    }

    override fun assign(pics: Assignment): Exp {
        val ret = Propagator.propagate(f, this, pics)
        val sRet = ret.maybeSimplify()
        return sRet
    }

    override fun maybeSimplify(): Exp = if (disjoint) {
        this
    } else {
        Propagator.propagate(f, constraint, lits)
    }

    fun careLits(): LitAnd {
        return if (disjoint) LitAnd.empty()
        else lits.careLits(constraint.vars)
    }

    override val exps: Iterable<Exp>
        get() = listOf(constraint, lits)


    override fun simplify(lits: Assignment): Exp {
        throw UnsupportedOperationException()
    }

    override val vars: Set<Var> get() = constraint.vars + lits.vars

    override val expFactory: ExpFactory
        get() = try {
            constraint.expFactory
        } catch (e: Exception) {
            lits.expFactory
        }


}
