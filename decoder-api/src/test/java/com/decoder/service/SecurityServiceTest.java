package com.decoder.service;

import com.decoder.model.Building;
import com.decoder.model.User;
import com.decoder.repository.BuildingRepository;
import com.decoder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityService (RBAC).
 * Tests role-based access control logic.
 */
@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private BuildingRepository buildingRepository;
    
    @InjectMocks
    private SecurityService securityService;
    
    private User adminUser;
    private User ownerUser;
    private Building building1;
    private Building building2;
    
    @BeforeEach
    void setUp() {
        adminUser = new User(1L, "admin", User.Role.ADMIN);
        ownerUser = new User(2L, "owner1", User.Role.OWNER);
        
        building1 = new Building(1L, "Building A", 2L, "123 Main St");
        building2 = new Building(2L, "Building B", 3L, "456 Oak Ave");
    }
    
    @Test
    void testHasAccessToBuilding_AdminUser_HasAccessToAllBuildings() {
        // Arrange
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building1));
        
        // Act
        boolean hasAccess = securityService.hasAccessToBuilding("admin", 1L);
        
        // Assert
        assertTrue(hasAccess);
        verify(userRepository).findByUsername("admin");
    }
    
    @Test
    void testHasAccessToBuilding_OwnerUser_HasAccessToOwnBuilding() {
        // Arrange
        when(userRepository.findByUsername("owner1")).thenReturn(Optional.of(ownerUser));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building1));
        
        // Act
        boolean hasAccess = securityService.hasAccessToBuilding("owner1", 1L);
        
        // Assert
        assertTrue(hasAccess); // owner1 owns building1
        verify(userRepository).findByUsername("owner1");
    }
    
    @Test
    void testHasAccessToBuilding_OwnerUser_NoAccessToOtherBuilding() {
        // Arrange
        when(userRepository.findByUsername("owner1")).thenReturn(Optional.of(ownerUser));
        when(buildingRepository.findById(2L)).thenReturn(Optional.of(building2));
        
        // Act
        boolean hasAccess = securityService.hasAccessToBuilding("owner1", 2L);
        
        // Assert
        assertFalse(hasAccess); // owner1 does not own building2
        verify(userRepository).findByUsername("owner1");
    }
    
    @Test
    void testGetAccessibleBuildingIds_AdminUser_ReturnsAllBuildings() {
        // Arrange
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(buildingRepository.findAll()).thenReturn(List.of(building1, building2));
        
        // Act
        List<Long> accessibleIds = securityService.getAccessibleBuildingIds("admin");
        
        // Assert
        assertEquals(2, accessibleIds.size());
        assertTrue(accessibleIds.contains(1L));
        assertTrue(accessibleIds.contains(2L));
    }
    
    @Test
    void testGetAccessibleBuildingIds_OwnerUser_ReturnsOnlyOwnBuildings() {
        // Arrange
        when(userRepository.findByUsername("owner1")).thenReturn(Optional.of(ownerUser));
        when(buildingRepository.findByOwnerId(2L)).thenReturn(List.of(building1));
        
        // Act
        List<Long> accessibleIds = securityService.getAccessibleBuildingIds("owner1");
        
        // Assert
        assertEquals(1, accessibleIds.size());
        assertTrue(accessibleIds.contains(1L));
        assertFalse(accessibleIds.contains(2L));
    }
}
