package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.security.MessageDigest

Message processData(Message message) {
    def reader = message.getBody(java.io.Reader)
    def jsonSlurper = new JsonSlurper()
    def payload = jsonSlurper.parse(reader)

    String paymentId = payload.paymentTransactionId ?: "TXN-DEFAULT-00"
    String sourceAccount = payload.debtorAccountNumber ?: "ACC-US-DEBTOR-01"
    String destinationAccount = payload.creditorAccountNumber ?: "ACC-GLOBAL-CREDITOR-99"
    double amount = (payload.settlementAmount ?: 0.0) as Double
    String currency = payload.currencyCode ?: "USD"
    String headerIdempKey = message.getProperty("Prop_Idempotency_Key") as String
    boolean isSimulatedRetry = payload.isSimulatedNetworkRetry ?: false

    if (amount <= 0.0) {
        throw new IllegalArgumentException("FINTECH_PAYLOAD_INVALID: Transaction settlement amount must be strictly greater than zero.")
    }

    String rawPayloadJson = JsonOutput.toJson(payload)
    MessageDigest digest = MessageDigest.getInstance("SHA-256")
    byte[] hashBytes = digest.digest(rawPayloadJson.getBytes("UTF-8"))
    StringBuilder hexString = new StringBuilder()
    for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b)
        if (hex.length() == 1) hexString.append('0')
        hexString.append(hex)
    }
    String payloadHash = hexString.toString()

    String effectiveIdempKey = (headerIdempKey && headerIdempKey != "IDEMP-NONE") ? headerIdempKey : "IDEMP_${paymentId}_${payloadHash.take(16)}"

    boolean isDuplicate = isSimulatedRetry || (payload.forceDuplicateRejection == true)

    message.setProperty("Prop_Payment_ID", paymentId)
    message.setProperty("Prop_Debtor_Account", sourceAccount)
    message.setProperty("Prop_Creditor_Account", destinationAccount)
    message.setProperty("Prop_Settlement_Amount", amount)
    message.setProperty("Prop_Currency", currency)
    message.setProperty("Prop_Payload_Hash", payloadHash)
    message.setProperty("Prop_Idempotency_Key", effectiveIdempKey)
    message.setProperty("Prop_Is_Duplicate", isDuplicate.toString())
    message.setProperty("Prop_Value_Date", payload.valueDate ?: new Date().format("yyyy-MM-dd"))

    return message
}
