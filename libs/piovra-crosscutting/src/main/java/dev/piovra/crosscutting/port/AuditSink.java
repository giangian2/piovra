package dev.piovra.crosscutting.port;

import java.time.Instant;

/** Destination of the audit log. Append-only: an entry is never modified and never deleted. */
public interface AuditSink {

    void record(AuditEntry entry);

    record AuditEntry(
            String action,
            String target,
            String actor,
            String tenantId,
            boolean succeeded,
            String failureReason,
            Instant occurredAt) {}
}
