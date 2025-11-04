package com.decoder.service;

import com.decoder.model.Building;
import com.decoder.model.User;
import com.decoder.repository.BuildingRepository;
import com.decoder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to handle RBAC (Role-Based Access Control) checks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityService {
    
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    
    /**
     * Verify if user has access to a building.
     * Admin users can access all buildings.
     * Owner users can only access their own buildings.
     */
    public boolean hasAccessToBuilding(String username, Long buildingId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityException("User not found: " + username));
        
        if (user.getRole() == User.Role.ADMIN) {
            log.debug("Admin user {} has access to building {}", username, buildingId);
            return true;
        }
        
        // Owner users can only access buildings they own
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found: " + buildingId));
        
        boolean hasAccess = building.getOwnerId().equals(user.getId());
        log.debug("Owner user {} access to building {}: {}", username, buildingId, hasAccess);
        
        return hasAccess;
    }
    
    /**
     * Get all building IDs accessible by the user.
     */
    public List<Long> getAccessibleBuildingIds(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityException("User not found: " + username));
        
        if (user.getRole() == User.Role.ADMIN) {
            // Admin sees all buildings
            return buildingRepository.findAll().stream()
                    .map(Building::getId)
                    .toList();
        }
        
        // Owner sees only their buildings
        return buildingRepository.findByOwnerId(user.getId()).stream()
                .map(Building::getId)
                .toList();
    }
}
