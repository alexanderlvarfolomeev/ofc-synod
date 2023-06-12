package ru.varfolomeev.actors

import akka.actor.Props
import ru.varfolomeev.messages.*

class Collector(private val processCount: Int) : ReceiveAbstractActor("collector") {
    private val decisions = Array(processCount) { 0L }
    private var decision: Int? = null

    private var decisionCount = 0
    private var crashCount = 0

    @OnReceive
    fun receive(processDecided: ProcessDecided) {
        decision = decision?.also {
            if (it != processDecided.value) {
                log.error("Different decision results.")
            }
        } ?: processDecided.value
        if (decisions[processDecided.processId] == 0L) {
            decisionCount++
        } else {
            log.error("Duplicate message: {}", processDecided)
        }
        decisions[processDecided.processId] = processDecided.nanoTime
    }

    @OnReceive
    fun receive(processCrashed: ProcessCrashed) {
        if (decisions[processCrashed.processId] == 0L) {
            crashCount++
        } else {
            log.error("Duplicate message: {}", processCrashed)
        }
        decisions[processCrashed.processId] = -1L
    }

    @OnReceive
    fun receive(check: CollectorCheck) {
        if (decisionCount + crashCount == processCount) {
            log.info("All Decided.")
            sender.tell(AllDecided(decision!!, decisions.max(), decisionCount), self)
        } else {
            log.info("Decided {} of {} processes.", decisionCount, processCount)
            sender.tell(InProgress, self)
        }
    }

    companion object {
        fun props(processCount: Int): Props =
            Props.create(Collector::class.java, processCount)
    }
}