package com.fhir.notification;

public record PatientPushNotificationCreateRequest(
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
}
