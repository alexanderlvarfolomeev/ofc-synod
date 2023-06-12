package ru.varfolomeev.messages

sealed class OuterMessage : LoggedMessage()

object Launch : OuterMessage()

data class Crash(val crashProbability: Double) : OuterMessage()

data class ProcessCrashed(val processId: Int) : OuterMessage()

data class ProcessDecided(val processId: Int, val value: Int, val nanoTime: Long) : OuterMessage()