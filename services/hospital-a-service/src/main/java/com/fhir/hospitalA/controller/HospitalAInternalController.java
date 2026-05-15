package com.fhir.hospitalA.controller;

import com.fhir.hospitalA.model.HospitalAPatient;
import com.fhir.hospitalA.service.HospitalAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/hospitalA")
public class HospitalAInternalController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    @Autowired
    private HospitalAService hospitalAService;

    @Value("${app.internal.service-token}")
    private String internalServiceToken;

    @PostMapping("/fhir-bundle")
    public String pullFhirBundle(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @RequestBody PullBundleRequest request) {
        requireInternalToken(serviceToken);
        return hospitalAService.pullFhirBundle(
                request.patientId(),
                request.consentToken(),
                request.scope());
    }

    @GetMapping("/patients/{identifier}")
    public HospitalPatientResponse findPatient(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @PathVariable String identifier) {
        requireInternalToken(serviceToken);
        HospitalAPatient patient = hospitalAService.findPatient(identifier)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hospital A patient not found: " + identifier));
        return new HospitalPatientResponse(
                patient.getPatientId(),
                patient.getAbhaId(),
                patient.getName(),
                patient.getDob(),
                patient.getGender());
    }

    @PostMapping("/patients")
    public HospitalPatientResponse linkPatient(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @RequestBody HospitalPatientRequest request) {
        requireInternalToken(serviceToken);
        HospitalAPatient patient = hospitalAService.linkPatient(
                request.patientId(),
                request.abhaId(),
                request.fullName(),
                request.dateOfBirth(),
                request.gender());
        return new HospitalPatientResponse(
                patient.getPatientId(),
                patient.getAbhaId(),
                patient.getName(),
                patient.getDob(),
                patient.getGender());
    }

    @GetMapping("/patients/{abhaId}/consults/exists")
    public Map<String, Boolean> hasConsults(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @PathVariable String abhaId) {
        requireInternalToken(serviceToken);
        return Map.of("exists", hospitalAService.hasConsults(abhaId));
    }

    @GetMapping("/patients/{abhaId}/consultations")
    public List<HospitalAService.InternalConsultationSummary> consultations(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @PathVariable String abhaId) {
        requireInternalToken(serviceToken);
        return hospitalAService.getInternalConsultationSummaries(abhaId);
    }

    private void requireInternalToken(String serviceToken) {
        if (!internalServiceToken.equals(serviceToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service token.");
        }
    }

    public record PullBundleRequest(String patientId, String consentToken, Set<String> scope) {
    }

    public record HospitalPatientRequest(
            String patientId,
            String abhaId,
            String fullName,
            String dateOfBirth,
            String gender) {
    }

    public record HospitalPatientResponse(
            String patientId,
            String abhaId,
            String fullName,
            String dateOfBirth,
            String gender) {
    }
}
