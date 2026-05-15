package com.fhir.patient.service;

import com.fhir.hie.client.HospitalDataClient;
import com.fhir.patient.dto.PatientConsultationSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientConsultationService {

    @Autowired
    private HospitalDataClient hospitalDataClient;

    public List<PatientConsultationSummaryDTO> getConsultations(String abhaId) {
        return hospitalDataClient.getConsultationSummaries(abhaId);
    }
}
