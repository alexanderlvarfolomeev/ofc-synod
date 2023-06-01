package ru.varfolomeev.messages

sealed interface CollectorMessage

object CollectorCheck

data class AllDecided(val value: Int, val end: Long) : CollectorMessage

object InProgress : CollectorMessage