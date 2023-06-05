package ru.varfolomeev

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.AskableActorRef
import akka.util.Timeout
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import ru.varfolomeev.actors.Collector
import ru.varfolomeev.actors.Process
import ru.varfolomeev.messages.*
import ru.varfolomeev.messages.LeaderElectionMessage.*
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class Main(private val processCount: Int, faultyCount: Int, private val faultProbability: Double) : AutoCloseable {
    private val system = ActorSystem.create("process-system-${toString().let { it.substring(it.length - 8) }}")
    private var leaderNumber: Int = 0
    private val processes = ArrayList<ActorRef>(processCount)
    private val faults: List<ActorRef>
    private val corrects: List<ActorRef>
    private val collector = AskableActorRef(system.actorOf(Collector.props(processCount, faultyCount), "collector"))

    init {
        val latch = CountDownLatch(1)
        processes.addAll((0 until processCount).map {
            system.actorOf(Props.create(Process::class.java) {
                latch.await()
                Process(it, processes)
            }, "process-$it")
        })

        latch.countDown()

        val shuffled = processes.shuffled(Random)
        faults = shuffled.take(faultyCount)
        corrects = shuffled.takeLast(processCount - faultyCount)

        system.eventStream.subscribe(collector.actorRef(), Decided::class.java)
    }

    fun run() {
        val start = System.nanoTime()

        broadcastMessage(Launch, processes, ActorRef.noSender())

        broadcastMessage(Crash(faultProbability), faults, ActorRef.noSender())

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
                    system.log().info(
                        "{} of {} process decided: {} in {} ms",
                        r.count,
                        processCount,
                        r.value,
                        (r.end - start) * 1e-6
                    )
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
        corrects.forEachIndexed { idx, actor ->
            actor.tell(if (leaderNumber % corrects.size == idx) Leader else Hold, ActorRef.noSender())
        }

        leaderNumber++
    }

    override fun close() {
        system.terminate()
        Await.result(system.whenTerminated(), Duration.Inf()).also {
            print("Waiting for loggers... ")
            (LoggerFactory.getILoggerFactory() as LoggerContext).stop()
            println("Complete.")
        }
    }
}

fun main(args: Array<String>) {
    Main(args[0].toInt(), args[1].toInt(), args[2].toDouble()).use {
        it.run()
    }
}
