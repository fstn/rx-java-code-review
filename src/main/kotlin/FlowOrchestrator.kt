package fstn.vertxFlow.core.utils

import fstn.vertxFlow.core.context.AllLinksConditionChecker
import fstn.vertxFlow.core.dynamic.MessageBody
import fstn.vertxFlow.core.model.ServiceTaskResult
import fstn.vertxFlow.core.model.node.Node
import rx.Observable
import rx.lang.kotlin.toObservable

/**
 * @author fstn
 */
object FlowOrchestrator : Loggify {

    fun getValidNextEvent(node: Node, messageBody: MessageBody): List<String> {
        if (logger.isDebugEnabled)
            logger.debug("${this.hashCode()}: Try to check condition on node ${node.id}")
        return AllLinksConditionChecker.getValidNextEvent(node, messageBody)
    }

    fun tryToDoNode(validEvents: MutableList<String> = mutableListOf(),
                    node: Node, messageBody: MessageBody)
            : Observable<ServiceTaskResult> {
        if (validEvents.isEmpty())
            validEvents.addAll(FlowOrchestrator.getValidNextEvent(node, messageBody))
        return if (validEvents.isNotEmpty()) {
            validEvents.toObservable().flatMap { notifyNextNode(node, it, messageBody) }
        } else {
            Observable.empty()
        }
    }

    private fun notifyNextNode(node: Node, it: String, messageBody: MessageBody): Observable<ServiceTaskResult>? {
        if (logger.isDebugEnabled)
            logger.debug("${this.hashCode()}: The condition is valid for node ${node.id}, sending event $it with messageBody $messageBody")
        return Observable.just(ServiceTaskResult(it, messageBody))
    }

    fun replayCurrentNode(node: Node, messageBody: MessageBody): Observable<ServiceTaskResult>? {
        val eventName = node.previousLinks.first().eventName
        if (logger.isDebugEnabled)
            logger.debug("${this.hashCode()}: The condition is not valid for node ${node.id}, sending event $eventName with messageBody $messageBody")
        return Observable.just(ServiceTaskResult(eventName, messageBody))
    }
}