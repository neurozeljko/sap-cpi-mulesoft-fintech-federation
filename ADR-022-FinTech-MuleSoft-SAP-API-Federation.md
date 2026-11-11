# ADR-022: FinTech Omni-Channel API Federation (SAP APIM & MuleSoft Anypoint)

## Status
Accepted

## Context
A global FinTech bank processes 5M transactions daily across MuleSoft Anypoint (Experience Layer) and SAP S/4HANA (Core Banking). Transient mobile network retries threaten multi-million dollar ledger double-entries. Fragmented telemetry prevents single-pane incident observability.

## Decision
1. Peer MuleSoft API Manager and SAP API Management over a dedicated, zero-trust mTLS 1.3 tunnel.
2. Ingest and harmonize MuleSoft B3 trace headers into W3C OpenTelemetry traceparent context in flight for unified Datadog and SAP Cloud ALM observability.
3. Enforce an in-flight SHA-256 composite idempotency engine in SAP CPI to intercept and suppress duplicate payment retries within 15 milliseconds.
4. Execute financial postings into S/4HANA Finance utilizing standard Clean Core OData V4 APIs.

## Consequences
- Guarantees absolute financial non-contamination with zero duplicate General Ledger entries.
- Provides unified cross-iPaaS distributed tracing across competing enterprise stacks.
- Preserves full Clean Core compliance with zero custom ABAP modifications.
