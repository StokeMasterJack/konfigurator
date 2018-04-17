package org.smartsoft.konfigurator

enum class Tri {
    TRUE, FALSE, OPEN;

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