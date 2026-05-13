package com.fhir.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationAuditClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String notificationAuditBaseUrl;
    private final String internalServiceToken;

    public NotificationAuditClient(
            @Value("${app.services.notification-audit.url}") String notificationAuditBaseUrl,
            @Value("${app.internal.service-token}") String internalServiceToken) {
        this.notificationAuditBaseUrl = notificationAuditBaseUrl;
        this.internalServiceToken = internalServiceToken;
    }

    public void createPatientPushNotification(PatientPushNotification notification) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken);

        PatientPushNotificationCreateRequest request = PatientPushNotificationCreateRequest.from(notification);
        HttpEntity<PatientPushNotificationCreateRequest> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(
                    notificationAuditBaseUrl + "/internal/notifications/patient-push",
                    entity,
                    PatientPushNotification.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to create notification in notification-audit-service.", ex);
        }
    }

    private record PatientPushNotificationCreateRequest(
            String exchangeId,
            String patientAbhaId,
            String patientName,
            String targetDoctorUsername,
            String sourceHospitalId,
            String targetHospitalId,
            String hospitalCode,
            String dataTypes,
            String fhirBundleHash,
            String fhirBundleJson) {

        private static PatientPushNotificationCreateRequest from(PatientPushNotification notification) {
            return new PatientPushNotificationCreateRequest(
                    notification.getExchangeId(),
                    notification.getPatientAbhaId(),
                    notification.getPatientName(),
                    notification.getTargetDoctorUsername(),
                    notification.getSourceHospitalId(),
                    notification.getTargetHospitalId(),
                    notification.getHospitalCode(),
                    notification.getDataTypes(),
                    notification.getFhirBundleHash(),
                    notification.getFhirBundleJson());
        }
    }
}
