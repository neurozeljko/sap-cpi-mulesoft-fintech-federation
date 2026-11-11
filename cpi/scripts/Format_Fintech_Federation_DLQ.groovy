package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

Message processData(Message message) {
    def exception = message.getProperty("CamelExceptionCaught")
    String paymentId = message.getProperty("Prop_Payment_ID") ?: "UNKNOWN_PAYMENT"
    String traceId = message.getProperty("Prop_Unified_Trace_ID") ?: "UNKNOWN_TRACE"
    String correlationId = message.getHeader("SAP_MplCorrelationId", String) ?: UUID.randomUUID().toString()
    String errorMsg = exception ? exception.getMessage() : "FinTech API federation transaction processing fault."

    def dlqPayload = [
        fintechFederationDeadLetterQueue: [
            incidentTrackingId: correlationId,
            paymentTransactionId: paymentId,
            openTelemetryTraceId: traceId,
            financialIntegrityRisk: "SETTLEMENT_ABORTED_NO_LEDGER_CONTAMINATION",
            failureCause: errorMsg,
            remediationPolicy: "QUARANTINE_IN_BTP_DLQ_AND_ALERT_TREASURY_OPS",
            quarantineTimestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ]
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(dlqPayload)))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("X-FinTech-Status", "QUARANTINED_IN_DLQ")
    return message
}
