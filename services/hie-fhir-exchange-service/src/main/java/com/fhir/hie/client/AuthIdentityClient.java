package com.fhir.hie.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

@Component
public class AuthIdentityClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestClient restClient;
    private final String internalServiceToken;

    public AuthIdentityClient(
            @Value("${auth.identity.base-url:http://localhost:8081}") String baseUrl,
            @Value("${app.internal.service-token}") String internalServiceToken) {
        this.restClient = RestClient.create(baseUrl);
        this.internalServiceToken = internalServiceToken;
    }

    public boolean patientExists(String abhaId) {
        try {
            PatientExistsResponse response = restClient.get()
                    .uri("/internal/identity/patients/{abhaId}/exists", abhaId)
                    .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                    .retrieve()
                    .body(PatientExistsResponse.class);
            return response != null && response.exists();
        } catch (RestClientException ex) {
            return false;
        }
    }

    public Optional<PatientProfile> findPatientByAbhaId(String abhaId) {
        try {
            PatientProfile patient = restClient.get()
                    .uri("/internal/identity/patients/{abhaId}", abhaId)
                    .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                    .retrieve()
                    .body(PatientProfile.class);
            return Optional.ofNullable(patient);
        } catch (RestClientResponseException ex) {
            if (isNotFound(ex.getStatusCode())) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    public void linkPatient(String abhaId, String hospitalId, String localPatientId) {
        restClient.post()
                .uri("/internal/identity/patient-links")
                .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                .body(new PatientLinkRequest(abhaId, hospitalId, localPatientId))
                .retrieve()
                .toBodilessEntity();
    }

    private boolean isNotFound(HttpStatusCode statusCode) {
        return statusCode != null && statusCode.value() == 404;
    }

    private record PatientExistsResponse(boolean exists) {
    }

    public record PatientProfile(
            String abhaId,
            String username,
            String fullName,
            String email,
            String phone,
            String gender,
            String dateOfBirth,
            String bloodGroup) {
    }

    private record PatientLinkRequest(String abhaId, String hospitalId, String localPatientId) {
    }
}
