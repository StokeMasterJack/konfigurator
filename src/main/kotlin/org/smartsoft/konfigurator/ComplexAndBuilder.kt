package org.smartsoft.konfigurator

class ComplexAndBuilder(private val list: MutableList<NonAnd> = mutableListOf()) : MutableList<NonAnd> by list {

    override fun add(element: NonAnd): Boolean {
        check(element !is And)
        return list.add(element)
    }

    override fun addAll(elements: Collection<NonAnd>): Boolean {
        throw UnsupportedOperationException()
    }

    fun addAll(exps: Iterable<NonAnd>) {
        for (complex in exps) {
            add(complex)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun mk(f: VarSet): Exp {
        return ComplexAnd(list)
    }

}