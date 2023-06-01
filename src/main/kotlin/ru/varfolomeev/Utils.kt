package ru.varfolomeev

import akka.actor.ActorRef

fun <T> Array<T>.mapInPlace(transform: (T) -> T) {
    for (i in this.indices) {
        this[i] = transform(this[i])
    }
}


fun broadcastMessage(message: Any, actors: Iterable<ActorRef>, sender: ActorRef?) {
    actors.forEach {
        it.tell(message, sender)
    }
}