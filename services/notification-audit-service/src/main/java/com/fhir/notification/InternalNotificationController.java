package com.fhir.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final PatientPushNotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final String internalServiceToken;

    public InternalNotificationController(
            PatientPushNotificationRepository notificationRepository,
            NotificationService notificationService,
            @Value("${app.internal.service-token}") String internalServiceToken) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.internalServiceToken = internalServiceToken;
    }

    @Transactional
    @PostMapping("/patient-push")
    public PatientPushNotification createPatientPushNotification(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @RequestBody PatientPushNotificationCreateRequest request) {
        if (!internalServiceToken.equals(serviceToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service token.");
        }

        PatientPushNotification notification = new PatientPushNotification();
        notification.setExchangeId(request.exchangeId());
        notification.setPatientAbhaId(request.patientAbhaId());
        notification.setPatientName(request.patientName());
        notification.setTargetDoctorUsername(request.targetDoctorUsername());
        notification.setSourceHospitalId(request.sourceHospitalId());
        notification.setTargetHospitalId(request.targetHospitalId());
        notification.setHospitalCode(request.hospitalCode());
        notification.setDataTypes(request.dataTypes());
        notification.setFhirBundleHash(request.fhirBundleHash());
        notification.setFhirBundleJson(request.fhirBundleJson());
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    @PostMapping("/consent-request")
    public void notifyConsentRequest(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @RequestBody ConsentNotificationRequest request) {
        if (!internalServiceToken.equals(serviceToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service token.");
        }

        notificationService.notifyPatientConsentRequest(request.patientId(), request.consentRequestId());
    }

    private record ConsentNotificationRequest(String patientId, Long consentRequestId) {
    }
}
