package com.fhir.hospitalB.controller;

import com.fhir.hospitalB.model.HospitalBPatient;
import com.fhir.hospitalB.service.HospitalBService;
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
@RequestMapping("/internal/hospitalB")
public class HospitalBInternalController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    @Autowired
    private HospitalBService hospitalBService;

    @Value("${app.internal.service-token}")
    private String internalServiceToken;

    @PostMapping("/fhir-bundle")
    public String pullFhirBundle(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @RequestBody PullBundleRequest request) {
        requireInternalToken(serviceToken);
        return hospitalBService.pullFhirBundle(
                request.patientId(),
                request.consentToken(),
                request.scope());
    }

    @GetMapping("/patients/{identifier}")
    public HospitalPatientResponse findPatient(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @PathVariable String identifier) {
        requireInternalToken(serviceToken);
        HospitalBPatient patient = hospitalBService.findPatient(identifier)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hospital B patient not found: " + identifier));
        return new HospitalPatientResponse(
                patient.getPatientId(),
                patient.getAbhaId(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getGender());
    }

    @PostMapping("/patients")
    public HospitalPatientResponse linkPatient(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @RequestBody HospitalPatientRequest request) {
        requireInternalToken(serviceToken);
        HospitalBPatient patient = hospitalBService.linkPatient(
                request.patientId(),
                request.abhaId(),
                request.fullName(),
                request.dateOfBirth(),
                request.gender());
        return new HospitalPatientResponse(
                patient.getPatientId(),
                patient.getAbhaId(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getGender());
    }

    @GetMapping("/patients/{abhaId}/consults/exists")
    public Map<String, Boolean> hasConsults(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @PathVariable String abhaId) {
        requireInternalToken(serviceToken);
        return Map.of("exists", hospitalBService.hasConsults(abhaId));
    }

    @GetMapping("/patients/{abhaId}/consultations")
    public List<HospitalBService.InternalConsultationSummary> consultations(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @PathVariable String abhaId) {
        requireInternalToken(serviceToken);
        return hospitalBService.getInternalConsultationSummaries(abhaId);
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
