package org.smartsoft.konfigurator

class SingletonIterator<out T>(private val singleElement: T) : Iterator<T> {

    private var _hasNext: Boolean = true

    override fun hasNext(): Boolean {
        return _hasNext
    }

    override fun next(): T {
        val retVal = singleElement
        _hasNext = false
        return retVal
    }

}

fun <T> T.mkSingletonIterable(): Iterable<T> {
    return object : Iterable<T> {
        override fun iterator() = SingletonIterator<T>(this@mkSingletonIterable)
    }
}