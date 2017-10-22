package fstn.vertxFlow.core.common.serviceTask

import fstn.vertxFlow.core.dynamic.MessageBody
import fstn.vertxFlow.core.model.EventBusEvent
import fstn.vertxFlow.core.model.ServiceTask
import fstn.vertxFlow.core.model.ServiceTaskResult
import fstn.vertxFlow.core.model.node.HumanNode
import fstn.vertxFlow.core.utils.Loggify
import io.vertx.rxjava.core.eventbus.Message
import rx.Observable

/**
 * @author fstn
 */
class HumanServiceTask : ServiceTask(), Loggify {
    private lateinit var humanNode: HumanNode

    override fun init() {
        if (node !is HumanNode) throw HumanServiceTaskMustBeLinkToHumanNodeException(node.id)
        humanNode = node as HumanNode
    }

    override fun checkValidity(): Boolean {
        if (humanNode.responseList.isEmpty()) throw HumanServiceTaskMustHaveTxtOrMediaException(humanNode.responseList.all().joinToString(","))
        return true
    }

    override fun safeHandle(message: Message<MessageBody>, messageBody: MessageBody): Observable<ServiceTaskResult> {
        messageBody.updateWithNode(node)
        return Observable.just(ServiceTaskResult(EventBusEvent.SET_MSG.name, messageBody))
    }

    class HumanServiceTaskMustBeLinkToHumanNodeException(id: String) : RuntimeException("Human service task must be link to human node $id")
    class HumanServiceTaskMustHaveTxtOrMediaException(txt: String) : RuntimeException("Human service task must have txt or media $txt")

}