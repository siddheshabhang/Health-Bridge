package com.fhir.hie.client;

import com.fhir.patient.dto.PatientConsultationSummaryDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class HospitalDataClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";
    private static final String HOSPITAL_A_ID = "HOSP-A";
    private static final String HOSPITAL_B_ID = "HOSP-B";

    private final RestClient hospitalAClient;
    private final RestClient hospitalBClient;
    private final String internalServiceToken;

    public HospitalDataClient(
            @Value("${hospital-a.service.base-url:http://localhost:8083}") String hospitalABaseUrl,
            @Value("${hospital-b.service.base-url:http://localhost:8084}") String hospitalBBaseUrl,
            @Value("${app.internal.service-token}") String internalServiceToken) {
        this.hospitalAClient = RestClient.create(hospitalABaseUrl);
        this.hospitalBClient = RestClient.create(hospitalBBaseUrl);
        this.internalServiceToken = internalServiceToken;
    }

    public Optional<HospitalPatient> findPatient(String hospitalId, String identifier) {
        HospitalRoute route = routeForHospitalId(hospitalId);
        try {
            HospitalPatient patient = route.client().get()
                    .uri(route.internalBasePath() + "/patients/{identifier}", identifier)
                    .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                    .retrieve()
                    .body(HospitalPatient.class);
            return Optional.ofNullable(patient);
        } catch (RestClientResponseException ex) {
            if (isNotFound(ex.getStatusCode())) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    public void linkPatient(String hospitalId, HospitalPatient patient) {
        HospitalRoute route = routeForHospitalId(hospitalId);
        route.client().post()
                .uri(route.internalBasePath() + "/patients")
                .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                .body(patient)
                .retrieve()
                .toBodilessEntity();
    }

    public boolean localPatientIdExists(String hospitalId, String localPatientId) {
        return findPatient(hospitalId, localPatientId).isPresent();
    }

    public boolean hasConsults(String hip, String abhaId) {
        HospitalRoute route = routeForHip(hip);
        try {
            ConsultationExists response = route.client().get()
                    .uri(route.internalBasePath() + "/patients/{abhaId}/consults/exists", abhaId)
                    .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                    .retrieve()
                    .body(ConsultationExists.class);
            return response != null && response.exists();
        } catch (RestClientException ex) {
            return false;
        }
    }

    public List<PatientConsultationSummaryDTO> getConsultationSummaries(String abhaId) {
        List<PatientConsultationSummaryDTO> summaries = new ArrayList<>();
        summaries.addAll(getConsultationSummaries(routeForHospitalId(HOSPITAL_A_ID), abhaId));
        summaries.addAll(getConsultationSummaries(routeForHospitalId(HOSPITAL_B_ID), abhaId));
        summaries.sort(Comparator.comparing(
                PatientConsultationSummaryDTO::getRecordedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed());
        return summaries;
    }

    private List<PatientConsultationSummaryDTO> getConsultationSummaries(HospitalRoute route, String abhaId) {
        try {
            List<HospitalConsultationSummary> summaries = route.client().get()
                    .uri(route.internalBasePath() + "/patients/{abhaId}/consultations", abhaId)
                    .headers(headers -> headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return summaries != null
                    ? summaries.stream().map(HospitalConsultationSummary::toDto).toList()
                    : List.of();
        } catch (RestClientException ex) {
            return List.of();
        }
    }

    private HospitalRoute routeForHospitalId(String hospitalId) {
        if (HOSPITAL_A_ID.equals(hospitalId)) {
            return new HospitalRoute(hospitalAClient, "/internal/hospitalA");
        }
        if (HOSPITAL_B_ID.equals(hospitalId)) {
            return new HospitalRoute(hospitalBClient, "/internal/hospitalB");
        }
        throw new IllegalArgumentException("Unsupported hospital: " + hospitalId);
    }

    private HospitalRoute routeForHip(String hip) {
        if ("HospitalA".equalsIgnoreCase(hip)) {
            return routeForHospitalId(HOSPITAL_A_ID);
        }
        if ("HospitalB".equalsIgnoreCase(hip)) {
            return routeForHospitalId(HOSPITAL_B_ID);
        }
        throw new IllegalArgumentException("Unsupported HIP: " + hip);
    }

    private boolean isNotFound(HttpStatusCode statusCode) {
        return statusCode != null && statusCode.value() == 404;
    }

    private record HospitalRoute(RestClient client, String internalBasePath) {
    }

    private record ConsultationExists(boolean exists) {
    }

    public record HospitalPatient(
            String patientId,
            String abhaId,
            String fullName,
            String dateOfBirth,
            String gender) {
    }

    public record HospitalConsultationSummary(
            String id,
            String hospitalName,
            String doctorName,
            String visitDate,
            String recordedAt,
            String clinicalNotes,
            String bloodPressure,
            String temperature,
            boolean prescriptionAvailable) {

        PatientConsultationSummaryDTO toDto() {
            return new PatientConsultationSummaryDTO(
                    id,
                    hospitalName,
                    doctorName,
                    visitDate,
                    recordedAt,
                    clinicalNotes,
                    bloodPressure,
                    temperature,
                    prescriptionAvailable);
        }
    }
}
