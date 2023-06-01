package ru.varfolomeev.messages

sealed class LeaderElectionMessage {
    override fun toString(): String {
        return this::class.simpleName!!
    }
}

object Hold : LeaderElectionMessage()

object Leader : LeaderElectionMessage()