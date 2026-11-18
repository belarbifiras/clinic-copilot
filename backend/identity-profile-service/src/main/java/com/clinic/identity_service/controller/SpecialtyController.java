package com.clinic.identity_service.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinic.identity_service.dto.SpecialtyResponse;
import com.clinic.identity_service.repository.SpecialtyRepository;

@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyController(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @GetMapping
    public List<SpecialtyResponse> getAll() {
        return specialtyRepository.findAll().stream()
                .map(s -> new SpecialtyResponse(s.getSpecialtyId(), s.getName()))
                .toList();
    }
}