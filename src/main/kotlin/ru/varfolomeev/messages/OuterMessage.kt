package ru.varfolomeev.messages

import akka.actor.ActorRef

sealed interface OuterMessage

object Launch : OuterMessage

data class Decided(val processId: Int, val value: Int, val nanoTime: Long) : OuterMessage

data class Crash(val crashProbability: Double) : OuterMessage