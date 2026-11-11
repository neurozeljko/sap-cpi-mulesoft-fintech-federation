package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

Message processData(Message message) {
    String paymentId = message.getProperty("Prop_Payment_ID")
    String idempKey = message.getProperty("Prop_Idempotency_Key")
    String traceId = message.getProperty("Prop_Unified_Trace_ID")
    String correlationId = message.getProperty("Prop_Correlation_ID") ?: UUID.randomUUID().toString()

    def idempotentAckPayload = [
        fintechIdempotentAcknowledgment: [
            status: "DUPLICATE_PAYMENT_INTERCEPTED_AND_SUPPRESSED",
            statusCode: 200,
            paymentTransactionId: paymentId,
            idempotencyLockKey: idempKey,
            openTelemetryTraceId: traceId,
            mplCorrelationId: correlationId,
            mitigationReason: "Payment order with identical hash and idempotency token is already committed in S/4HANA Finance General Ledger. Duplicate execution prevented.",
            acknowledgedTimestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ]
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(idempotentAckPayload)))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("X-Idempotency-Execution", "SUPPRESSED_DUPLICATE_ACKNOWLEDGED")
    return message
}
