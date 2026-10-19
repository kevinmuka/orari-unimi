package app.orariunimi

import kotlinx.coroutines.Job

/** Keeps a completed request from changing the screen after a newer navigation action. */
internal class NavigationRequestGate {
    private var generation = 0L
    private var activeJob: Job? = null

    fun next(): Long {
        activeJob?.cancel()
        activeJob = null
        return ++generation
    }

    fun track(token: Long, job: Job) {
        if (isCurrent(token)) activeJob = job else job.cancel()
    }

    fun isCurrent(token: Long): Boolean = token == generation
}
