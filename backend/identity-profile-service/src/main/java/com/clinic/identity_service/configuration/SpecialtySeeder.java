package com.clinic.identity_service.configuration;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.clinic.identity_service.entity.Specialty;
import com.clinic.identity_service.repository.SpecialtyRepository;

@Component
public class SpecialtySeeder implements CommandLineRunner {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtySeeder(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @Override
    public void run(String... args) {
        if (specialtyRepository.count() > 0) {
            return;
        }
        List<Specialty> specialties = List.of(
                "General Medicine", "Cardiology", "Dermatology", "Pediatrics",
                "Neurology", "Orthopedics", "Psychiatry", "Gynecology",
                "Ophthalmology", "Dentistry"
        ).stream().map(name -> {
            Specialty s = new Specialty();
            s.setName(name);
            return s;
        }).toList();

        specialtyRepository.saveAll(specialties);
    }
}