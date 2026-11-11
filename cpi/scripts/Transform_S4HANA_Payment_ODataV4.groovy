package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

Message processData(Message message) {
    String paymentId = message.getProperty("Prop_Payment_ID")
    String debtorAcc = message.getProperty("Prop_Debtor_Account")
    String creditorAcc = message.getProperty("Prop_Creditor_Account")
    double amount = message.getProperty("Prop_Settlement_Amount") as Double
    String currency = message.getProperty("Prop_Currency")
    String idempKey = message.getProperty("Prop_Idempotency_Key")
    String traceId = message.getProperty("Prop_Unified_Trace_ID")
    String valueDate = message.getProperty("Prop_Value_Date")
    String correlationId = message.getProperty("Prop_Correlation_ID") ?: UUID.randomUUID().toString()

    def s4JournalEntry = [
        CompanyCode: "1010",
        DocumentDate: valueDate,
        PostingDate: valueDate,
        AccountingDocumentType: "KZ",
        DocumentHeaderText: "MULE-FED-${paymentId}".take(25),
        CreatedByUser: "BTP_FINTECH_APIM",
        to_Item: [
            [
                ReferenceDocumentItem: "1",
                GLAccount: "0011310000",
                AmountInTransactionCurrency: -amount,
                TransactionCurrency: currency,
                DebitCreditCode: "H",
                DocumentItemText: "Debit ${debtorAcc.take(15)}"
            ],
            [
                ReferenceDocumentItem: "2",
                GLAccount: "0011320000",
                AmountInTransactionCurrency: amount,
                TransactionCurrency: currency,
                DebitCreditCode: "S",
                DocumentItemText: "Credit ${creditorAcc.take(15)}"
            ]
        ],
        federationAuditMetadata: [
            idempotencyLockKey: idempKey,
            openTelemetryTraceId: traceId,
            originatingGateway: "MULESOFT_ANYPOINT_EDGE",
            mplCorrelationTrackingId: correlationId,
            settlementTimestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ]
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(s4JournalEntry)))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("X-CleanCore-Target", "S4HANA_FINANCE_ODATA_V4")
    message.setHeader("X-Idempotency-Execution", "COMMITTED_NO_DUPLICATE")
    return message
}
