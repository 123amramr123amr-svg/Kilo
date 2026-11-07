package com.personalaiagent.core

/** Injectable clock so time-dependent logic is testable. */
fun interface TimeProvider {
    fun now(): Long
}

/** Injectable id factory so persisted entities can be deterministic in tests. */
fun interface IdGenerator {
    fun newId(): String
}

object DefaultIdGenerator : IdGenerator {
    override fun newId(): String = java.util.UUID.randomUUID().toString()
}

object SystemTimeProvider : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
