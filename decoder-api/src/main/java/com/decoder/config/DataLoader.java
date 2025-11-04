package com.decoder.config;

import com.decoder.model.Building;
import com.decoder.model.User;
import com.decoder.repository.BuildingRepository;
import com.decoder.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data loader that runs after application context is initialized.
 * Populates initial data for users and buildings.
 * Uses native SQL queries to work around SQLite's lack of getGeneratedKeys support.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Loading initial data...");
        
        // Create users if they don't exist using native SQL to avoid getGeneratedKeys issue
        if (userRepository.findByUsername("admin").isEmpty()) {
            entityManager.createNativeQuery(
                "INSERT INTO users (username, role) VALUES ('admin', 'ADMIN')"
            ).executeUpdate();
            log.info("Created admin user");
        }
        
        if (userRepository.findByUsername("owner1").isEmpty()) {
            entityManager.createNativeQuery(
                "INSERT INTO users (username, role) VALUES ('owner1', 'OWNER')"
            ).executeUpdate();
            log.info("Created owner1 user");
        }
        
        if (userRepository.findByUsername("owner2").isEmpty()) {
            entityManager.createNativeQuery(
                "INSERT INTO users (username, role) VALUES ('owner2', 'OWNER')"
            ).executeUpdate();
            log.info("Created owner2 user");
        }
        
        // Flush and refresh to ensure data is available
        entityManager.flush();
        entityManager.clear();
        
        // Get owner1 and owner2 IDs for buildings
        User owner1 = userRepository.findByUsername("owner1").orElseThrow();
        User owner2 = userRepository.findByUsername("owner2").orElseThrow();
        
        // Create buildings if they don't exist
        if (buildingRepository.count() == 0) {
            entityManager.createNativeQuery(
                String.format("INSERT INTO buildings (name, owner_id, address) VALUES ('Building A', %d, '123 Main St')", 
                    owner1.getId())
            ).executeUpdate();
            log.info("Created Building A");
            
            entityManager.createNativeQuery(
                String.format("INSERT INTO buildings (name, owner_id, address) VALUES ('Building B', %d, '456 Oak Ave')", 
                    owner1.getId())
            ).executeUpdate();
            log.info("Created Building B");
            
            entityManager.createNativeQuery(
                String.format("INSERT INTO buildings (name, owner_id, address) VALUES ('Building C', %d, '789 Pine Rd')", 
                    owner2.getId())
            ).executeUpdate();
            log.info("Created Building C");
        }
        
        log.info("Initial data loading completed");
    }
}

