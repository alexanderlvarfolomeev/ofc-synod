package ru.varfolomeev.actors

import akka.actor.DeadLetter
import akka.actor.Props
import ru.varfolomeev.messages.AllDecided
import ru.varfolomeev.messages.CollectorCheck
import ru.varfolomeev.messages.Decided
import ru.varfolomeev.messages.InProgress

class Collector(private val processCount: Int) : ReceiveAbstractActor("collector") {
    private var count = 0
    private val decisions = Array(processCount) { 0L }
    private var decision: Int? = null

    @OnReceive
    fun receive(decided: Decided) {
        decision = decision?.also {
            if (it != decided.value) {
                log.error("Different decision results.")
            }
        } ?: decided.value
        if (decisions[decided.processId] == 0L) {
            count++
        } else {
            log.error("Duplicate message: {}", decided)
        }
        decisions[decided.processId] = decided.nanoTime
    }

    @OnReceive
    fun receive(check: CollectorCheck) {
        if (count == processCount) {
            log.info("All Decided.")
            sender.tell(AllDecided(decision!!, decisions.max()), self)
        } else {
            log.info("Decided {} of {} processes.", count, processCount)
            sender.tell(InProgress, self)
        }
    }

    companion object {
        fun props(processCount: Int): Props =
            Props.create(Collector::class.java, processCount)
    }
}