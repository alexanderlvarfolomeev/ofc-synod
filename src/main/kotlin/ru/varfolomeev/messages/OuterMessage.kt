package ru.varfolomeev.messages

sealed class OuterMessage : LoggedMessage()

object Launch : OuterMessage()

data class Crash(val crashProbability: Double) : OuterMessage()

data class Decided(val processId: Int, val value: Int, val nanoTime: Long) : OuterMessage()