package org.smartsoft.konfigurator

enum class Tri {
    TRUE, FALSE, OPEN;

    fun toBool(): Boolean = when (this) {
        TRUE -> true
        FALSE -> false
        OPEN -> throw IllegalStateException()
    }

    fun toBoolOrNull(): Boolean? = when (this) {
        TRUE -> true
        FALSE -> false
        OPEN -> null
    }

    companion object {
        fun fromBoolean(b: Boolean?): Tri = when {
            b == null -> OPEN
            b -> TRUE
            !b -> FALSE
            else -> throw IllegalStateException()
        }
    }

    val isAssigned: Boolean get() = this == TRUE || this == FALSE
}