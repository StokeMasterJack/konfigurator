package org.smartsoft.konfigurator

class LitIt(private val mapIt: Iterator<Map.Entry<Var, Boolean>>) : Iterator<Lit> {

    override fun hasNext() = mapIt.hasNext()

    override fun next() = mapIt.next().toLit1()
}