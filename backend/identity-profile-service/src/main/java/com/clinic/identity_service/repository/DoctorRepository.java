package com.clinic.identity_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinic.identity_service.entity.Doctor;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    
}
