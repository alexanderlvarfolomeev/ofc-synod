package ru.varfolomeev.actors

import akka.actor.ActorRef
import ru.varfolomeev.actors.ProcessCondition.*
import ru.varfolomeev.actors.ProcessCondition.ProposeCondition.*
import ru.varfolomeev.broadcastMessage
import ru.varfolomeev.mapInPlace
import ru.varfolomeev.messages.*
import ru.varfolomeev.messages.LeaderElectionMessage.*
import ru.varfolomeev.messages.ProposeMessage.*
import kotlin.random.Random

class Process(
    private val processId: Int,
    private val processes: List<ActorRef>
) : ReceiveAbstractActor("process#$processId") {
    private var condition: ProcessCondition = Valid()
    private val fixedProposal: Int = Random.nextInt(2)

    private var ballot: Long = (processId - processes.size).toLong()
    private var proposal: Int? = null
    private var readballot: Long = -1
    private var imposeballot: Long = ballot
    private var estimate: Int? = null
    private val states = Array<Pair<Int?, Long>>(processes.size) { null to -1 }

    private var stateCount = 0
    private var ackCount = 0
    private val majority = processes.size / 2 + 1

    @OnReceive
    fun receive(crash: Crash) {
        when (val cond = condition) {
            Crashed -> {
                log.error("Crashing crashed process: {}", self.path().name())
            }

            is FaultProne -> {
                log.error("Crashing fault-prone process: {}", self.path().name())
            }

            is Valid -> {
                condition = FaultProne(crash.crashProbability, cond.proposeCondition)
            }
        }
    }

    @OnReceive
    fun receive(launch: Launch) {
        when (val cond = condition) {
            Crashed -> {
                log.error("Launching crashed process: {}", self.path().name())
            }

            is FaultProne -> {
                propose(fixedProposal, cond)
            }

            is Valid -> {
                propose(fixedProposal, cond)
            }
        }
    }

    @OnReceive
    fun receive(leader: Leader) {
        when (val cond = condition) {
            Crashed -> {
                log.error("Election of crashed process: {}", self.path().name())
            }

            is FaultProne -> {
                log.error("Election of fault-prone process: {}", self.path().name())
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
        if (ballot == abort.ballot && it.proposeCondition == PROPOSE) {
            when (it.leader) {
                true -> {
                    propose(proposal!!, it)
                }

                false -> {
                    it.proposeCondition = AWAIT_PROPOSE
                }
            }
        }
    }

    @OnReceive
    fun receive(gather: Gather) = wrapProposeEvent {
        if (ballot == gather.ballot) {
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
        if (ballot == ack.ballot) {
            ackCount++
            if (ackCount >= majority) {
                ackCount = 0
                broadcast(Decide(proposal!!))
            }
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
        ballot += processes.size
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

    private fun wrapProposeEvent(event: (NotCrashed) -> Unit) =
        when (val cond = condition) {
            Crashed -> {
            }

            is FaultProne -> {
                if (Random.nextDouble() < cond.crashProbability) {
                    condition = Crashed
                } else {
                    event(cond)
                }
            }

            is Valid -> {
                event(cond)
            }
        }
}