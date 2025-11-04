package com.decoder.service;

import com.decoder.model.Reading;
import com.decoder.model.dto.ForecastResponse;
import com.decoder.repository.ReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ForecastService.
 * Tests the forecasting logic (moving average calculation and recommendations).
 */
@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {
    
    @Mock
    private ReadingRepository readingRepository;
    
    @InjectMocks
    private ForecastService forecastService;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(forecastService, "energyThreshold", 100.0);
    }
    
    @Test
    void testGenerateForecast_WithReadings_ReturnsForecast() {
        // Arrange
        Long buildingId = 1L;
        int forecastMinutes = 60;
        
        List<Reading> recentReadings = Arrays.asList(
                createReading(1L, "sensor1", LocalDateTime.now().minusMinutes(10), 50.0),
                createReading(1L, "sensor1", LocalDateTime.now().minusMinutes(9), 55.0),
                createReading(1L, "sensor1", LocalDateTime.now().minusMinutes(8), 60.0),
                createReading(1L, "sensor1", LocalDateTime.now().minusMinutes(7), 58.0),
                createReading(1L, "sensor1", LocalDateTime.now().minusMinutes(6), 62.0)
        );
        
        when(readingRepository.findLastReadingsByBuildingId(any(), any()))
                .thenReturn(recentReadings);
        
        // Act
        ForecastResponse response = forecastService.generateForecast(buildingId, forecastMinutes);
        
        // Assert
        assertNotNull(response);
        assertEquals(buildingId, response.getBuildingId());
        assertEquals(forecastMinutes, response.getForecast().size());
        assertNotNull(response.getRecommendation());
        
        // Verify moving average is calculated (approximately 57.0)
        assertTrue(response.getForecast().get(0).getValue() > 50.0);
        assertTrue(response.getForecast().get(0).getValue() < 65.0);
    }
    
    @Test
    void testGenerateForecast_ExceedsThreshold_ReturnsRecommendation() {
        // Arrange
        Long buildingId = 1L;
        int forecastMinutes = 60;
        
        List<Reading> highReadings = Arrays.asList(
                createReading(1L, "sensor1", LocalDateTime.now().minusMinutes(10), 120.0),
                createReading(1L, "sensor1", LocalDateTime.now().minusMinutes(9), 125.0),
                createReading(1L, "sensor1", LocalDateTime.now().minusMinutes(8), 130.0),
                createReading(1L, "sensor1", LocalDateTime.now().minusMinutes(7), 128.0),
                createReading(1L, "sensor1", LocalDateTime.now().minusMinutes(6), 132.0)
        );
        
        when(readingRepository.findLastReadingsByBuildingId(any(), any()))
                .thenReturn(highReadings);
        
        // Act
        ForecastResponse response = forecastService.generateForecast(buildingId, forecastMinutes);
        
        // Assert
        assertNotNull(response);
        assertTrue(response.getRecommendation().contains("energy-saving mode") || 
                   response.getRecommendation().contains("exceeds threshold"));
    }
    
    @Test
    void testGenerateForecast_NoReadings_ReturnsEmptyForecast() {
        // Arrange
        Long buildingId = 1L;
        int forecastMinutes = 60;
        
        when(readingRepository.findLastReadingsByBuildingId(any(), any()))
                .thenReturn(List.of());
        
        // Act
        ForecastResponse response = forecastService.generateForecast(buildingId, forecastMinutes);
        
        // Assert
        assertNotNull(response);
        assertEquals(buildingId, response.getBuildingId());
        assertTrue(response.getRecommendation().contains("Insufficient data"));
    }
    
    private Reading createReading(Long buildingId, String sensorId, 
                                   LocalDateTime timestamp, Double value) {
        Reading reading = new Reading();
        reading.setBuildingId(buildingId);
        reading.setSensorId(sensorId);
        reading.setTimestamp(timestamp);
        reading.setValue(value);
        return reading;
    }
}
