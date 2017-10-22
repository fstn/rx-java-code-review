package fstn.vertxFlow.core.model

import fstn.vertxFlow.core.dynamic.MessageBody
import fstn.vertxFlow.core.manager.EventBusConsumerManager
import fstn.vertxFlow.core.model.node.Node
import fstn.vertxFlow.core.rx.helper.subscribeAndLog
import fstn.vertxFlow.core.utils.FlowOrchestrator
import fstn.vertxFlow.core.utils.Loggify
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.eventbus.EventBus
import io.vertx.rxjava.core.eventbus.Message
import rx.Observable

/**
 * Service task declaration
 */
abstract class ServiceTask : Handler<Message<MessageBody>>, Loggify {
    val properties: MutableList<Property> = mutableListOf()
    lateinit var node: Node
    lateinit var eventBus: EventBus
    lateinit var vertx: Vertx
    lateinit var eventName: String

    companion object {
        val EVENT_ENDED = "EVENT_ENDED"
    }

    override fun handle(message: Message<MessageBody>) {
        logger.info("Handle event message ${message.body()} for event: $eventName for nodeId ${node.id}")
        val messageBody = message.body()
        safeHandle(message,
                messageBody)
                .subscribe(
                        { serviceTaskResult ->
                            send(serviceTaskResult.eventName, serviceTaskResult.messageBody)
                        },
                        {
                            if (it != null) throw it
                            else {
                                message.reply(null)
                                ko()
                            }
                        },
                        {
                            message.reply(null)
                            ok(messageBody).subscribeAndReply(message)
                        }
                )
    }

    abstract fun safeHandle(message: Message<MessageBody>, messageBody: MessageBody): Observable<ServiceTaskResult>

    abstract fun checkValidity(): Boolean

    abstract fun init()

    /**
     * Send event to other nodes
     */
    private fun <T> send(eventName: String, messageBody: T): rx.Observable<Message<T>> {
        return eventBus.rxSend<T>(eventName, messageBody)
                .toObservable()
    }

    /**
     * Call when Service Task job is done
     * Will getValidNextEvent all nextLink to see if there is a valid condition
     */
    private fun ok(messageBody: MessageBody): Observable<ServiceTaskResult> {

        if (logger.isDebugEnabled) {
            logger.debug("Ok called for node ${node.id}")
        }

        send<JsonObject>(EVENT_ENDED, JsonObject().put("eventName", eventName).put("nodeId", node.id)).subscribeAndLog()

        return FlowOrchestrator.tryToDoNode(node = node, messageBody = messageBody)
    }

    /**
     * Stop the call chains here
     */
    private fun ko() {

        if (logger.isDebugEnabled) {
            logger.debug("Dropped for node ${node.id}")
        }
        send<JsonObject>(EVENT_ENDED, JsonObject().put("eventName", eventName).put("nodeId", node.id)).subscribeAndLog()
    }

    /**
     * Register inside eventBus
     */
    fun register(eventBus: EventBus, flowName: String, eventName: String, node: Node) {
        logger.info("Try to Register ${this::class.simpleName} for event $eventName")
        this.node = node
        this.eventBus = eventBus
        this.eventName = eventName
        init()
        checkValidity()
        EventBusConsumerManager.register(flowName, eventName, this)
    }


    private fun Observable<ServiceTaskResult>.subscribeAndReply(message: Message<MessageBody>) {
        this.subscribe(
                reply(message),
                fail(message),
                complete())
    }

    private fun reply(message: Message<MessageBody>): (ServiceTaskResult) -> Unit =
            { message.reply("") }

    private fun fail(message: Message<MessageBody>): (Throwable) -> Unit =
            { message.fail(0, it.message) }

    private fun complete() = {}

}

