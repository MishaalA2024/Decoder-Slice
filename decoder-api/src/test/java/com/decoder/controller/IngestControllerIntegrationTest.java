package com.decoder.controller;

import com.decoder.model.Reading;
import com.decoder.model.dto.ReadingRequest;
import com.decoder.repository.ReadingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for IngestController.
 * Tests the full API endpoint including database interaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IngestControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ReadingRepository readingRepository;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Test
    void testIngestReading_Success() throws Exception {
        // Arrange
        ReadingRequest request = new ReadingRequest();
        request.setBuildingId(1L);
        request.setSensorId("sensor-001");
        request.setTimestamp(LocalDateTime.now().format(FORMATTER));
        request.setValue(75.5);
        
        // Act & Assert
        mockMvc.perform(post("/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.buildingId").value(1))
                .andExpect(jsonPath("$.sensorId").value("sensor-001"))
                .andExpect(jsonPath("$.value").value(75.5));
        
        // Verify database
        Reading saved = readingRepository.findAll().stream()
                .filter(r -> r.getSensorId().equals("sensor-001"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(saved);
        assertEquals(1L, saved.getBuildingId());
        assertEquals(75.5, saved.getValue());
    }
    
    @Test
    void testIngestReading_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange - missing required fields
        ReadingRequest request = new ReadingRequest();
        request.setBuildingId(1L);
        // Missing sensorId, timestamp, value
        
        // Act & Assert
        mockMvc.perform(post("/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
