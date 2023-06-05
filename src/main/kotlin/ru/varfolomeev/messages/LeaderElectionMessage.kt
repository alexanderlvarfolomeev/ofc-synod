package ru.varfolomeev.messages

sealed class LeaderElectionMessage : LoggedMessage() {

    object Hold : LeaderElectionMessage()

    object Leader : LeaderElectionMessage()
}
