package com.clinic.identity_service.configuration;

import com.clinic.identity_service.entity.Role;
import com.clinic.identity_service.entity.User;
import com.clinic.identity_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Initialise automatiquement un compte administrateur au démarrage
 * si aucun administrateur n'existe encore dans la base de données.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "admin@clinic.tn";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setFirstName("Super");
            admin.setLastName("Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("Admin1234!"));
            admin.setDateOfBirth(LocalDate.of(1990, 1, 1));
            admin.setRole(Role.ADMIN);
            admin.setIsActive(true);
            userRepository.save(admin);
            System.out.println(">>> [Clinic Copilot] Compte administrateur initialisé : " + adminEmail + " / Admin1234!");
        }
    }
}
