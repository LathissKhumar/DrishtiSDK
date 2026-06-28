package io.drishti.core

expect class Lock() {
    fun lock()
    fun unlock()
}

inline fun <T> Lock.withLock(action: () -> T): T {
    lock()
    try {
        return action()
    } finally {
        unlock()
    }
}
