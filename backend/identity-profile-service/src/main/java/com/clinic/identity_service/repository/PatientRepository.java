package com.clinic.identity_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinic.identity_service.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    
}
