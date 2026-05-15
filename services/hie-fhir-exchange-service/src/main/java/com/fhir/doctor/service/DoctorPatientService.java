package com.fhir.doctor.service;

import com.fhir.doctor.dto.DoctorPatientLookupResponseDTO;
import com.fhir.hie.client.AuthIdentityClient;
import com.fhir.hie.client.HospitalDataClient;
import com.fhir.shared.security.SecurityContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DoctorPatientService {

    private static final String HOSPITAL_A_ID = "HOSP-A";
    private static final String HOSPITAL_B_ID = "HOSP-B";
    private static final String HOSPITAL_A_PATIENT_PREFIX = "HA-P-";
    private static final String HOSPITAL_B_PATIENT_PREFIX = "HB-P-";

    @Autowired
    private AuthIdentityClient authIdentityClient;

    @Autowired
    private HospitalDataClient hospitalDataClient;

    @Autowired
    private SecurityContextHelper securityContextHelper;

    public DoctorPatientLookupResponseDTO lookupPatient(String identifier) {
        String doctorHospitalId = requireDoctorHospitalId();

        DoctorPatientLookupResponseDTO localMatch = findLocalPatient(identifier, doctorHospitalId);
        if (localMatch != null) {
            return localMatch;
        }

        return authIdentityClient.findPatientByAbhaId(identifier)
                .map(patient -> new DoctorPatientLookupResponseDTO(
                    null,
                    patient.abhaId(),
                    patient.fullName(),
                    patient.dateOfBirth(),
                    patient.gender(),
                    "GLOBAL"
                ))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient not found for identifier: " + identifier));
    }

    public Map<String, String> linkPatientByAbhaId(String abhaId) {
        String doctorHospitalId = requireDoctorHospitalId();

        DoctorPatientLookupResponseDTO existingLocalPatient = findLocalPatient(abhaId, doctorHospitalId);
        if (existingLocalPatient != null && "LOCAL".equals(existingLocalPatient.getSource())) {
            return Map.of(
                    "message", "Patient already linked to hospital",
                    "abhaId", abhaId,
                    "localPatientId", existingLocalPatient.getPatientId(),
                    "hospitalId", doctorHospitalId,
                    "fullName", valueOrEmpty(existingLocalPatient.getFullName())
            );
        }

        AuthIdentityClient.PatientProfile patient = authIdentityClient.findPatientByAbhaId(abhaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient with ABHA-ID not found"));

        String generatedPatientId = generateLocalPatientId(doctorHospitalId);
        authIdentityClient.linkPatient(abhaId, doctorHospitalId, generatedPatientId);
        hospitalDataClient.linkPatient(
                doctorHospitalId,
                new HospitalDataClient.HospitalPatient(
                        generatedPatientId,
                        abhaId,
                        patient.fullName(),
                        patient.dateOfBirth(),
                        patient.gender()));

        return Map.of(
                "message", "Patient linked to hospital successfully",
                "abhaId", abhaId,
                "localPatientId", generatedPatientId,
                "hospitalId", doctorHospitalId,
                "fullName", valueOrEmpty(patient.fullName())
        );
    }

    private String requireDoctorHospitalId() {
        String doctorHospitalId = securityContextHelper.extractHospitalId();
        if (doctorHospitalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor is not associated with a hospital");
        }
        return doctorHospitalId;
    }

    private String generateLocalPatientId(String hospitalId) {
        String prefix = switch (hospitalId) {
            case HOSPITAL_A_ID -> HOSPITAL_A_PATIENT_PREFIX;
            case HOSPITAL_B_ID -> HOSPITAL_B_PATIENT_PREFIX;
            default -> "PX-P-";
        };
        String candidate;
        do {
            int sequence = ThreadLocalRandom.current().nextInt(1000, 10000);
            candidate = prefix + sequence;
        } while (isLocalPatientIdTaken(hospitalId, candidate));
        return candidate;
    }

    private boolean isLocalPatientIdTaken(String hospitalId, String localPatientId) {
        return hospitalDataClient.localPatientIdExists(hospitalId, localPatientId);
    }

    private DoctorPatientLookupResponseDTO findLocalPatient(String identifier, String doctorHospitalId) {
        return hospitalDataClient.findPatient(doctorHospitalId, identifier)
                .map(patient -> new DoctorPatientLookupResponseDTO(
                        patient.patientId(),
                        patient.abhaId(),
                        patient.fullName(),
                        patient.dateOfBirth(),
                        patient.gender(),
                        "LOCAL"))
                .orElse(null);
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }
}
