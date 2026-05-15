package com.fhir.identity.controller;

import com.fhir.auth.service.PatientRegistrationService;
import com.fhir.identity.model.HospitalPatientLink;
import com.fhir.identity.service.IdentityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/internal/identity")
public class InternalIdentityController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final IdentityService identityService;
    private final PatientRegistrationService patientRegistrationService;
    private final String internalServiceToken;

    public InternalIdentityController(
            IdentityService identityService,
            PatientRegistrationService patientRegistrationService,
            @Value("${app.internal.service-token}") String internalServiceToken) {
        this.identityService = identityService;
        this.patientRegistrationService = patientRegistrationService;
        this.internalServiceToken = internalServiceToken;
    }

    @GetMapping("/patients/{abhaId}/exists")
    public Map<String, Boolean> patientExists(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @PathVariable String abhaId) {
        requireInternalToken(serviceToken);
        return Map.of("exists", identityService.exists(abhaId));
    }

    @GetMapping("/patients/{abhaId}")
    public Map<String, Object> getPatient(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @PathVariable String abhaId) {
        requireInternalToken(serviceToken);
        return patientRegistrationService.getPatientByAbhaId(abhaId);
    }

    @Transactional
    @PostMapping("/patient-links")
    public HospitalPatientLink linkPatient(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String serviceToken,
            @RequestBody PatientLinkRequest request) {
        requireInternalToken(serviceToken);
        return identityService.linkPatient(
                request.abhaId(),
                request.hospitalId(),
                request.localPatientId());
    }

    private void requireInternalToken(String serviceToken) {
        if (!internalServiceToken.equals(serviceToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service token.");
        }
    }

    public record PatientLinkRequest(String abhaId, String hospitalId, String localPatientId) {
    }
}
