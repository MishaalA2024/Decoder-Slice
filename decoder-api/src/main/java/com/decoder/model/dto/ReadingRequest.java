package com.decoder.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingRequest {
    @NotNull(message = "buildingId is required")
    private Long buildingId;
    
    @NotNull(message = "sensorId is required")
    private String sensorId;
    
    @NotNull(message = "timestamp is required")
    private String timestamp;
    
    @NotNull(message = "value is required")
    private Double value;
}
