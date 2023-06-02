package ru.varfolomeev

import ch.qos.logback.classic.AsyncAppender

class SleepAsyncAppender : AsyncAppender() {
    override fun stop() {
        if (!started) return

        while (numberOfElementsInQueue > 0) {
            Thread.sleep(1000)
        }

        Thread.sleep(10000)

        super.stop()
    }
}