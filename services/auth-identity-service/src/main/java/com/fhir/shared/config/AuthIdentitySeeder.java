package com.fhir.shared.config;

import com.fhir.shared.hospital.Hospital;
import com.fhir.shared.hospital.HospitalRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class AuthIdentitySeeder implements CommandLineRunner {

    private final HospitalRepository hospitalRepository;

    public AuthIdentitySeeder(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    @Override
    public void run(String... args) {
        ensureHospital("HOSP-A", "City General Hospital", "CGH-MUM", "Mumbai, Maharashtra", "admin@citygeneral.example");
        ensureHospital("HOSP-B", "Metro Medical Center", "MMC-DEL", "New Delhi, Delhi", "admin@metromedical.example");
    }

    private void ensureHospital(String id, String name, String code, String location, String email) {
        Hospital hospital = hospitalRepository.findById(id).orElseGet(Hospital::new);
        hospital.setId(id);
        hospital.setName(name);
        hospital.setCode(code);
        hospital.setLocation(location);
        hospital.setContactEmail(email);
        hospital.setActive(true);
        hospitalRepository.save(hospital);
    }
}
