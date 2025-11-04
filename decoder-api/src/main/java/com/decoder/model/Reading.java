package com.decoder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "readings", indexes = {
    @Index(name = "idx_building_timestamp", columnList = "buildingId,timestamp"),
    @Index(name = "idx_sensor_timestamp", columnList = "sensorId,timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long buildingId;
    
    @Column(nullable = false)
    private String sensorId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private Double value;
}
