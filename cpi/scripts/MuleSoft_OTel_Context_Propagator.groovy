package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message

Message processData(Message message) {
    String b3TraceId = message.getProperty("Prop_B3_TraceId") as String
    String b3SpanId = message.getProperty("Prop_B3_SpanId") as String
    String w3cTraceparent = message.getProperty("Prop_W3C_Traceparent") as String
    String correlationId = message.getProperty("Prop_Correlation_ID") as String

    String unifiedTraceId = (b3TraceId && b3TraceId != "TRACE-MULE-DEFAULT-00") ? b3TraceId.replace("-", "").padLeft(32, "0") : UUID.randomUUID().toString().replace("-", "")
    String unifiedSpanId = (b3SpanId && b3SpanId != "SPAN-MULE-01") ? b3SpanId.replace("-", "").take(16).padLeft(16, "0") : UUID.randomUUID().toString().replace("-", "").take(16)
    
    if (w3cTraceparent == "NONE" || !w3cTraceparent) {
        w3cTraceparent = "00-${unifiedTraceId}-${unifiedSpanId}-01"
    }

    message.setProperty("Prop_Unified_Trace_ID", unifiedTraceId)
    message.setProperty("Prop_Unified_Span_ID", unifiedSpanId)
    message.setProperty("Prop_Harmonized_Traceparent", w3cTraceparent)

    message.setHeader("traceparent", w3cTraceparent)
    message.setHeader("X-B3-TraceId", unifiedTraceId)
    message.setHeader("X-B3-SpanId", unifiedSpanId)
    message.setHeader("X-B3-Sampled", "1")
    message.setHeader("X-Datadog-Trace-ID", unifiedTraceId.take(16))
    message.setHeader("X-Datadog-Parent-ID", unifiedSpanId)
    message.setHeader("SAP-Passthrough-CorrelationId", correlationId)

    return message
}
