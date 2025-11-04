package com.decoder.service;

import com.decoder.model.Reading;
import com.decoder.model.dto.ForecastResponse;
import com.decoder.repository.ReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple forecasting service using moving average baseline model.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastService {
    
    private final ReadingRepository readingRepository;
    
    @Value("${decoder.forecasting.threshold:100.0}")
    private Double energyThreshold;
    
    private static final int MOVING_AVERAGE_WINDOW = 5;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * Generates a forecast for the next N minutes using moving average.
     * Returns recommendation if forecast exceeds threshold.
     */
    public ForecastResponse generateForecast(Long buildingId, int forecastMinutes) {
        log.debug("Generating forecast for building {} for next {} minutes", buildingId, forecastMinutes);
        
        // Get recent readings (last hour should be sufficient for baseline)
        LocalDateTime fromTimestamp = LocalDateTime.now().minusHours(1);
        List<Reading> recentReadings = readingRepository
                .findLastReadingsByBuildingId(buildingId, fromTimestamp);
        
        if (recentReadings.isEmpty()) {
            log.warn("No recent readings found for building {}", buildingId);
            return createEmptyForecast(buildingId, forecastMinutes);
        }
        
        // Calculate moving average
        Double movingAverage = calculateMovingAverage(recentReadings);
        
        // Generate forecast points (one per minute)
        List<ForecastResponse.ForecastPoint> forecastPoints = new ArrayList<>();
        LocalDateTime currentTime = LocalDateTime.now();
        
        for (int i = 1; i <= forecastMinutes; i++) {
            LocalDateTime forecastTime = currentTime.plusMinutes(i);
            ForecastResponse.ForecastPoint point = new ForecastResponse.ForecastPoint(
                    forecastTime.format(FORMATTER),
                    movingAverage // Simple baseline: use moving average
            );
            forecastPoints.add(point);
        }
        
        // Generate recommendation
        String recommendation = generateRecommendation(movingAverage);
        
        return new ForecastResponse(buildingId, forecastPoints, recommendation);
    }
    
    /**
     * Calculate simple moving average from recent readings.
     */
    private Double calculateMovingAverage(List<Reading> readings) {
        if (readings == null || readings.isEmpty()) {
            return 0.0;
        }
        
        int windowSize = Math.min(MOVING_AVERAGE_WINDOW, readings.size());
        
        // Take the most recent N readings
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < windowSize; i++) {
            Reading reading = readings.get(i);
            if (reading != null && reading.getValue() != null) {
                sum += reading.getValue();
                count++;
            }
        }
        
        return count > 0 ? sum / count : 0.0;
    }
    
    /**
     * Generate recommendation based on forecast threshold.
     */
    private String generateRecommendation(Double forecastValue) {
        if (forecastValue > energyThreshold) {
            return "Forecast exceeds threshold (" + forecastValue + " > " + energyThreshold + 
                   "). Recommendation: Activate energy-saving mode.";
        }
        return "Energy consumption within normal range. No action required.";
    }
    
    /**
     * Create empty forecast when no data is available.
     */
    private ForecastResponse createEmptyForecast(Long buildingId, int forecastMinutes) {
        List<ForecastResponse.ForecastPoint> emptyForecast = new ArrayList<>();
        return new ForecastResponse(buildingId, emptyForecast, 
                "Insufficient data for forecast. Collect more readings.");
    }
}
