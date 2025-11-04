package com.decoder.service;

import com.decoder.model.Reading;
import com.decoder.model.dto.ReadingRequest;
import com.decoder.model.dto.ReadingResponse;
import com.decoder.repository.ReadingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingService {
    
    private final ReadingRepository readingRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Transactional
    public ReadingResponse ingestReading(ReadingRequest request) {
        log.debug("Ingesting reading: buildingId={}, sensorId={}, value={}", 
                request.getBuildingId(), request.getSensorId(), request.getValue());
        
        Reading reading = new Reading();
        reading.setBuildingId(request.getBuildingId());
        reading.setSensorId(request.getSensorId());
        
        // Parse timestamp or use current time
        LocalDateTime timestamp;
        if (request.getTimestamp() != null && !request.getTimestamp().isEmpty()) {
            try {
                timestamp = LocalDateTime.parse(request.getTimestamp(), FORMATTER);
            } catch (Exception e) {
                log.warn("Invalid timestamp format, using current time: {}", request.getTimestamp());
                timestamp = LocalDateTime.now();
            }
        } else {
            timestamp = LocalDateTime.now();
        }
        reading.setTimestamp(timestamp);
        reading.setValue(request.getValue());
        
        // Use native SQL to avoid getGeneratedKeys() issue with SQLite
        String sql = "INSERT INTO readings (building_id, sensor_id, timestamp, value) VALUES (?, ?, ?, ?)";
        entityManager.createNativeQuery(sql)
                .setParameter(1, reading.getBuildingId())
                .setParameter(2, reading.getSensorId())
                .setParameter(3, reading.getTimestamp())
                .setParameter(4, reading.getValue())
                .executeUpdate();
        
        entityManager.flush();
        entityManager.clear();
        
        // Get the inserted ID using last_insert_rowid()
        Long insertedId = ((Number) entityManager.createNativeQuery("SELECT last_insert_rowid()")
                .getSingleResult()).longValue();
        
        reading.setId(insertedId);
        
        log.info("Reading saved with id: {}", reading.getId());
        
        return mapToResponse(reading);
    }
    
    public List<ReadingResponse> getLastReadings(Long buildingId, int minutes) {
        log.debug("Fetching last readings for building {} within last {} minutes", 
                buildingId, minutes);
        
        LocalDateTime fromTimestamp = LocalDateTime.now().minusMinutes(minutes);
        List<Reading> readings = readingRepository
                .findLastReadingsByBuildingId(buildingId, fromTimestamp);
        
        return readings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private ReadingResponse mapToResponse(Reading reading) {
        return new ReadingResponse(
                reading.getBuildingId(),
                reading.getSensorId(),
                reading.getTimestamp(),
                reading.getValue()
        );
    }
}
