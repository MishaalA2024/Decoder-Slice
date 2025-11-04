package com.decoder.controller;

import com.decoder.model.dto.ReadingRequest;
import com.decoder.model.dto.ReadingResponse;
import com.decoder.service.ReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
@Slf4j
public class IngestController {
    
    private final ReadingService readingService;
    
    @PostMapping
    public ResponseEntity<ReadingResponse> ingestReading(@Valid @RequestBody ReadingRequest request) {
        log.info("Received ingest request for buildingId: {}, sensorId: {}", 
                request.getBuildingId(), request.getSensorId());
        
        ReadingResponse response = readingService.ingestReading(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
