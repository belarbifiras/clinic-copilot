package com.clinic.identity_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinic.identity_service.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
    
}
