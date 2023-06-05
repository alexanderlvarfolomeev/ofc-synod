package ru.varfolomeev.messages

import java.util.*
import kotlin.reflect.full.memberProperties

open class LoggedMessage {
    final override fun toString(): String = (
            listOf(this::class.simpleName?.uppercase(Locale.getDefault())) +
                    this::class.memberProperties.map { it.getter.call(this).toString() }
            ).joinToString(prefix = "[", postfix = "]")
}