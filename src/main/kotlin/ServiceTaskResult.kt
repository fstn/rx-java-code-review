package fstn.vertxFlow.core.model

import fstn.vertxFlow.core.dynamic.MessageBody

/**
 * Result of the service task call
 * @author fstn
 */
class ServiceTaskResult(val eventName:String,val messageBody: MessageBody)