package ru.varfolomeev

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.DeadLetter
import akka.pattern.AskableActorRef
import akka.util.Timeout
import ru.varfolomeev.actors.Collector
import ru.varfolomeev.actors.Process
import ru.varfolomeev.messages.*
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class Main(private val processCount: Int) : AutoCloseable {
    private val system = ActorSystem.create("process-system-$this")
    private var leaderNumber: Int = 0
    private val processes = List<ActorRef>(processCount) { system.actorOf(Process.props(it, processCount),"process-$it") }
    private val collector = AskableActorRef(system.actorOf(Collector.props(processCount), "collector"))

    init {
        system.eventStream.subscribe(collector.actorRef(), Decided::class.java)

        broadcastMessage(PassRefs(processes), processes, ActorRef.noSender())
    }

    fun run() {
        val start = System.nanoTime()

        broadcastMessage(Launch, processes, ActorRef.noSender())

        val cancellable = system.scheduler().scheduleWithFixedDelay(
            Duration.Zero(),
            Duration.create(100, TimeUnit.MILLISECONDS),
            this::changeLeader,
            system.dispatcher()
        )

        while (
            when (val r = Await.result(
                collector.ask(
                    CollectorCheck,
                    Timeout.durationToTimeout(Duration.create(1, TimeUnit.MINUTES))
                ), Duration.Inf()
            )) {
                is AllDecided -> {
                    cancellable.cancel()
                    system.log().info("All process decided: {} in {} ms", r.value, (r.end - start) * 1e-6)
                    false
                }
                is InProgress -> {
                    true
                }
                else -> {
                    cancellable.cancel()
                    system.log().error("Unexpected message: {}", r)
                    false
                }
            }
        ) {
            Thread.sleep(1000)
        }
    }

    private fun changeLeader() {
        processes.forEachIndexed { idx, actor ->
            actor.tell(if (leaderNumber % processCount == idx) Leader else Hold, ActorRef.noSender())
        }

        leaderNumber++
    }

    override fun close() {
        system.terminate()
        print(Await.result(system.whenTerminated(), Duration.Inf()))
    }

    override fun toString(): String {
        val str = super.toString()
        return str.substring(str.length - 8)
    }
}

fun main(args: Array<String>) {
    Main(args[0].toInt()).use {
        it.run()
    }
}
