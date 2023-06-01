package ru.varfolomeev.actors

import akka.actor.ActorRef
import akka.actor.Props
import ru.varfolomeev.actors.ProcessCondition.*
import ru.varfolomeev.actors.ProcessCondition.ProposeCondition.*
import ru.varfolomeev.broadcastMessage
import ru.varfolomeev.mapInPlace
import ru.varfolomeev.messages.*

class Process(
    private val processId: Int,
    private val processCount: Int,
) : ReceiveAbstractActor("process#$processId") {
    private var processes: List<ActorRef> = listOf()
    private var condition: ProcessCondition = Valid()

    private var ballot: Long = (processId - processCount).toLong()
    private var proposal: Int? = null
    private var readballot: Long = -1
    private var imposeballot: Long = ballot
    private var estimate: Int? = null
    private val states = Array<Pair<Int?, Long>>(processCount) { null to -1 }
    private var stateCount = 0
    private var ackCount = 0

    private val majority = processCount / 2 + 1

    @OnReceive
    fun receive(pass: PassRefs) {
        processes = pass.processes

    }

    @OnReceive
    fun receive(launch: Launch) {
        when (val cond = condition) {
            Crashed -> {
                TODO()
            }

            is FaultProne -> {
                TODO()
            }

            is Valid -> {
                propose(0, cond)
            }
        }
    }

    @OnReceive
    fun receive(leader: Leader) {
        when (val cond = condition) {
            Crashed -> {
                TODO()
            }

            is FaultProne -> {
                TODO()
            }

            is Valid -> {
                cond.leader = true
                when (cond.proposeCondition) {
                    AWAIT_PROPOSE -> propose(proposal!!, cond)
                    else -> {
                    }
                }
            }
        }
    }

    @OnReceive
    fun receive(hold: Hold) {
        when (val cond = condition) {
            is Valid -> {
                cond.leader = false
            }

            else -> {
            }
        }
    }

    @OnReceive
    fun receive(propose: Propose) = wrapProposeEvent {
        initialize(propose.value)
        broadcast(Read(ballot))
    }

    @OnReceive
    fun receive(read: Read) = wrapProposeEvent {
        if (readballot > read.ballot || imposeballot > read.ballot) {
            tell(Abort(read.ballot), sender)
        } else {
            readballot = read.ballot
            tell(Gather(processId, read.ballot, imposeballot, estimate), sender)
        }
    }

    @OnReceive
    fun receive(abort: Abort) = wrapProposeEvent {
        when (it.leader) {
            true -> {
                propose(proposal!!, it)
            }

            false -> {
                it.proposeCondition = AWAIT_PROPOSE
            }
        }
    }

    @OnReceive
    fun receive(gather: Gather) = wrapProposeEvent {
        states[gather.processId] = gather.estimate to gather.estballot
        stateCount++
        if (stateCount >= majority) {
            val pair = states.maxBy { it.second }
            if (pair.second >= 0) {
                proposal = pair.first
            }
            states.mapInPlace { null to -1 }
            stateCount = 0
            broadcast(Impose(ballot, proposal!!))
        }
    }

    @OnReceive
    fun receive(impose: Impose) = wrapProposeEvent {
        if (readballot > impose.ballot || imposeballot > impose.ballot) {
            tell(Abort(impose.ballot), sender)
        } else {
            estimate = impose.value
            imposeballot = impose.ballot
            tell(Ack(impose.ballot), sender)
        }
    }

    @OnReceive
    fun receive(ack: Ack) = wrapProposeEvent {
        ackCount++
        if (ackCount >= majority) {
            broadcast(Decide(proposal!!))
            ackCount = 0
        }
    }

    @OnReceive
    fun receive(decide: Decide) = wrapProposeEvent {
        if (it.proposeCondition != DECIDED) {
            it.proposeCondition = DECIDED
            context.system.eventStream.publish(Decided(processId, decide.value, System.nanoTime()))
            broadcast(decide)
            log.info("Desided: {}", decide.value)
        }
    }

    private fun propose(value: Int, cond: NotCrashed) {
        cond.proposeCondition = PROPOSE
        tell(Propose(value), self)
    }

    private fun initialize(value: Int) {
        proposal = value
        ballot += processCount
        states.mapInPlace { null to -1 }
        stateCount = 0
        ackCount = 0
    }

    private fun tell(message: Any, receiver: ActorRef) {
        receiver.tell(message, self)
        log.info("Message {} sent to {}.", message, receiver.path().name().replace('-', '#'))
    }

    private fun broadcast(message: Any) {
        broadcastMessage(message, processes, self)
        log.info("Broadcasted: {}", message)
    }

    private fun wrapProposeEvent(event: (NotCrashed) -> Unit): Unit =
        when (val cond = condition) {
            Crashed -> {
            }

            is FaultProne -> {
                TODO()
            }

            is Valid -> {
                event(cond)
            }
        }

    companion object {
        fun props(processId: Int, processCount: Int): Props = Props.create(Process::class.java, processId, processCount)
    }
}