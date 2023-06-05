package ru.varfolomeev.messages

sealed class ProposeMessage : LoggedMessage() {

    data class Propose(val value: Int) : ProposeMessage()

    data class Read(val ballot: Long) : ProposeMessage()

    data class Abort(val ballot: Long) : ProposeMessage()

    data class Gather(val processId: Int, val ballot: Long, val estballot: Long, val estimate: Int?) : ProposeMessage()

    data class Impose(val ballot: Long, val value: Int) : ProposeMessage()

    data class Ack(val ballot: Long) : ProposeMessage()

    data class Decide(val value: Int) : ProposeMessage()
}
