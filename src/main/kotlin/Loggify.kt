package fstn.vertxFlow.core.utils

import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

interface Loggify {
    val logger: Logger
        get() {
            return LoggerFactory.getLogger(javaClass.toString())
        }
}
