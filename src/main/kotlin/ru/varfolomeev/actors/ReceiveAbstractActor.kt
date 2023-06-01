package ru.varfolomeev.actors

import akka.actor.AbstractActor
import akka.event.Logging
import akka.event.LoggingAdapter
import akka.japi.pf.ReceiveBuilder
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure

abstract class ReceiveAbstractActor(shortName: String) : AbstractActor() {
    val log: LoggingAdapter = Logging.getLogger(context?.system, shortName)

    override fun createReceive(): Receive = javaClass.kotlin.memberFunctions
        .filter { it.hasAnnotation<OnReceive>() }
        .foldRight(ReceiveBuilder()) { f, rb ->
            rb.match(f.parameters[1].type.jvmErasure.java) { m ->
                try {
                    log.info(
                        "Received message from {}: {}.",
                        if (sender == null) "No Sender" else sender.path().name().replace('-', '#'),
                        m.toString()
                    )
                    f.call(this, m)
                } catch (e: Exception) {
                    handleException(e)
                }
            }!!
        }.matchAny(this::default).build()

    private fun default(message: Any) {
        log.warning("Received unknown message: {}", message)
    }

    private fun handleException(e: Exception) {
        val exc = if (e is InvocationTargetException) e.cause else e
        log.error(
            "Exception was thrown: {} - {}",
            exc,
            exc?.message
        )
    }
}