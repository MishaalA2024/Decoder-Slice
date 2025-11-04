package com.decoder.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingResponse {
    private Long buildingId;
    private String sensorId;
    private LocalDateTime timestamp;
    private Double value;
}
