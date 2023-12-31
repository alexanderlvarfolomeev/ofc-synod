package ru.varfolomeev.actors

sealed interface ProcessCondition {
    sealed interface NotCrashed : ProcessCondition {
        val leader: Boolean
        var proposeCondition: ProposeCondition
    }

    data class Valid(override var leader: Boolean, override var proposeCondition: ProposeCondition) : NotCrashed {
        constructor() : this(true, ProposeCondition.NOT_STARTED)
    }

    data class FaultProne(val crashProbability: Double, override var proposeCondition: ProposeCondition) : NotCrashed {
        override val leader = false
    }

    object Crashed : ProcessCondition

    enum class ProposeCondition {
        NOT_STARTED, AWAIT_PROPOSE, PROPOSE, DECIDED
    }
}