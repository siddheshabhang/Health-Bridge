package com.fhir.hie.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationAuditClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestClient restClient;
    private final String internalServiceToken;

    public NotificationAuditClient(
            @Value("${notification-audit.service.base-url:http://localhost:8087}") String baseUrl,
            @Value("${app.internal.service-token}") String internalServiceToken) {
        this.restClient = RestClient.create(baseUrl);
        this.internalServiceToken = internalServiceToken;
    }

    public void notifyConsentRequest(String patientId, Long consentRequestId) {
        restClient.post()
                .uri("/internal/notifications/consent-request")
                .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                .body(new ConsentNotificationRequest(patientId, consentRequestId))
                .retrieve()
                .toBodilessEntity();
    }

    public Long logPendingTransfer(PendingTransferAuditRequest request) {
        TransferAuditResponse response = restClient.post()
                .uri("/internal/audit/transfers/pending")
                .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                .body(request)
                .retrieve()
                .body(TransferAuditResponse.class);
        if (response == null || response.id() == null) {
            throw new IllegalStateException("Notification/Audit did not return a transfer audit id.");
        }
        return response.id();
    }

    public void markTransferSuccess(Long id, String fhirBundlePayload, int bundleResourceCount) {
        restClient.patch()
                .uri("/internal/audit/transfers/{id}/success", id)
                .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                .body(new TransferSuccessRequest(fhirBundlePayload, bundleResourceCount))
                .retrieve()
                .toBodilessEntity();
    }

    public void markTransferFailed(Long id, String reason) {
        restClient.patch()
                .uri("/internal/audit/transfers/{id}/failure", id)
                .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                .body(new TransferFailureRequest(reason))
                .retrieve()
                .toBodilessEntity();
    }

    private record ConsentNotificationRequest(String patientId, Long consentRequestId) {
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

    private record TransferAuditResponse(Long id) {
    }

    private record TransferSuccessRequest(String fhirBundlePayload, int bundleResourceCount) {
    }

    private record TransferFailureRequest(String reason) {
    }
}
