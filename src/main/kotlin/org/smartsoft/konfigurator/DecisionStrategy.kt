package org.smartsoft.konfigurator

fun Complex.chooseDecisionVar(): Var {
    return when (this) {
        is ComplexAnd -> vars.first()
        is NonAndComplex -> vars.first()
        is MixedAnd -> constraint.chooseDecisionVar()
        else -> throw IllegalStateException()
    }
}