package com.decoder.controller;

import com.decoder.model.dto.ForecastResponse;
import com.decoder.model.dto.ReadingResponse;
import com.decoder.security.JwtAuthenticationToken;
import com.decoder.service.ForecastService;
import com.decoder.service.ReadingService;
import com.decoder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/buildings")
@RequiredArgsConstructor
@Slf4j
public class BuildingController {
    
    private final ReadingService readingService;
    private final ForecastService forecastService;
    private final SecurityService securityService;
    
    @GetMapping("/{id}/last-readings")
    public ResponseEntity<List<ReadingResponse>> getLastReadings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "60") int minutes,
            Authentication authentication) {
        
        String username = extractUsername(authentication);
        log.info("User {} requesting last readings for building {}", username, id);
        
        // RBAC check: verify user has access to this building
        if (!securityService.hasAccessToBuilding(username, id)) {
            log.warn("User {} does not have access to building {}", username, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<ReadingResponse> readings = readingService.getLastReadings(id, minutes);
        return ResponseEntity.ok(readings);
    }
    
    @GetMapping("/{id}/forecast")
    public ResponseEntity<ForecastResponse> getForecast(
            @PathVariable Long id,
            @RequestParam(defaultValue = "60") int minutes,
            Authentication authentication) {
        
        String username = extractUsername(authentication);
        log.info("User {} requesting forecast for building {}", username, id);
        
        // RBAC check: verify user has access to this building
        if (!securityService.hasAccessToBuilding(username, id)) {
            log.warn("User {} does not have access to building {}", username, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        ForecastResponse forecast = forecastService.generateForecast(id, minutes);
        return ResponseEntity.ok(forecast);
    }
    
    private String extractUsername(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getUsername();
        }
        return authentication != null ? authentication.getName() : "anonymous";
    }
}
