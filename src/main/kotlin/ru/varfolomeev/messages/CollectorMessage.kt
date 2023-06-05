package ru.varfolomeev.messages

sealed class CollectorMessage : LoggedMessage()

object CollectorCheck : LoggedMessage()

data class AllDecided(val value: Int, val end: Long, val count: Int) : CollectorMessage()

object InProgress : CollectorMessage()