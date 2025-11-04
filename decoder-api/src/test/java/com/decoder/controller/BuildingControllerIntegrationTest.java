package com.decoder.controller;

import com.decoder.model.Building;
import com.decoder.model.Reading;
import com.decoder.model.User;
import com.decoder.repository.BuildingRepository;
import com.decoder.repository.ReadingRepository;
import com.decoder.repository.UserRepository;
import com.decoder.security.JwtAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for BuildingController with RBAC.
 * Tests API endpoints with role-based access control and database interaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BuildingControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private BuildingRepository buildingRepository;
    
    @Autowired
    private ReadingRepository readingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private Building testBuilding;
    private User adminUser;
    private User ownerUser;
    
    @BeforeEach
    void setUp() {
        // Create test data
        adminUser = userRepository.save(new User(null, "admin", User.Role.ADMIN));
        ownerUser = userRepository.save(new User(null, "owner1", User.Role.OWNER));
        
        testBuilding = buildingRepository.save(
                new Building(null, "Test Building", ownerUser.getId(), "Test Address"));
        
        // Create some test readings
        Reading reading1 = new Reading(null, testBuilding.getId(), "sensor1", 
                LocalDateTime.now().minusMinutes(30), 50.0);
        Reading reading2 = new Reading(null, testBuilding.getId(), "sensor1", 
                LocalDateTime.now().minusMinutes(20), 55.0);
        readingRepository.save(reading1);
        readingRepository.save(reading2);
    }
    
    @Test
    void testGetLastReadings_AdminUser_Success() throws Exception {
        // Arrange - Set admin authentication
        Authentication auth = new JwtAuthenticationToken("admin");
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Act & Assert
        mockMvc.perform(get("/buildings/{id}/last-readings?minutes=60", testBuilding.getId())
                .header("Authorization", "Bearer admin:ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }
    
    @Test
    void testGetForecast_OwnerUser_Success() throws Exception {
        // Arrange - Set owner authentication
        Authentication auth = new JwtAuthenticationToken("owner1");
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Act & Assert
        mockMvc.perform(get("/buildings/{id}/forecast?minutes=60", testBuilding.getId())
                .header("Authorization", "Bearer owner1:OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildingId").value(testBuilding.getId()))
                .andExpect(jsonPath("$.forecast").isArray())
                .andExpect(jsonPath("$.recommendation").exists());
    }
    
    @Test
    void testGetLastReadings_UnauthorizedAccess_Forbidden() throws Exception {
        // Arrange - Try to access as different owner
        User otherOwner = userRepository.save(new User(null, "owner2", User.Role.OWNER));
        Building otherBuilding = buildingRepository.save(
                new Building(null, "Other Building", otherOwner.getId(), "Other Address"));
        
        Authentication auth = new JwtAuthenticationToken("owner1");
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Act & Assert - owner1 should not access owner2's building
        mockMvc.perform(get("/buildings/{id}/last-readings?minutes=60", otherBuilding.getId())
                .header("Authorization", "Bearer owner1:OWNER"))
                .andExpect(status().isForbidden());
    }
}
