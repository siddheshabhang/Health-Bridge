package com.fhir.shared.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/audit/transfers")
public class InternalTransferAuditController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final AuditService auditService;
    private final String internalServiceToken;

    public InternalTransferAuditController(
            AuditService auditService,
            @Value("${app.internal.service-token}") String internalServiceToken) {
        this.auditService = auditService;
        this.internalServiceToken = internalServiceToken;
    }

    @PostMapping("/pending")
    public TransferAuditResponse logPending(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @RequestBody PendingTransferAuditRequest request) {
        requireInternalToken(serviceToken);
        Long id = auditService.logPending(
                request.patientId(),
                request.sourceHospital(),
                request.targetHospital(),
                request.requesterUsername(),
                request.requesterHospitalId(),
                request.consentRequestId(),
                request.bundleResourceCount(),
                request.consentSnapshot(),
                request.exchangeType());
        return new TransferAuditResponse(id);
    }

    @PatchMapping("/{id}/success")
    public void markSuccess(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @PathVariable Long id,
            @RequestBody TransferSuccessRequest request) {
        requireInternalToken(serviceToken);
        auditService.markSuccess(id, request.fhirBundlePayload(), request.bundleResourceCount());
    }

    @PatchMapping("/{id}/failure")
    public void markFailure(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @PathVariable Long id,
            @RequestBody TransferFailureRequest request) {
        requireInternalToken(serviceToken);
        auditService.markFailed(id, request.reason());
    }

    private void requireInternalToken(String serviceToken) {
        if (!internalServiceToken.equals(serviceToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service token.");
        }
    }

    public record PendingTransferAuditRequest(
            String patientId,
            String sourceHospital,
            String targetHospital,
            String requesterUsername,
            String requesterHospitalId,
            Long consentRequestId,
            int bundleResourceCount,
            String consentSnapshot,
            String exchangeType) {
    }

    public record TransferAuditResponse(Long id) {
    }

    public record TransferSuccessRequest(String fhirBundlePayload, int bundleResourceCount) {
    }

    public record TransferFailureRequest(String reason) {
    }
}
