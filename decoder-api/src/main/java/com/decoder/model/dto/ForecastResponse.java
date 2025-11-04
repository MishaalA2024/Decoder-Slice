package com.decoder.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ForecastResponse {
    private Long buildingId;
    private List<ForecastPoint> forecast;
    private String recommendation;
    
    public ForecastResponse(Long buildingId, List<ForecastPoint> forecast, String recommendation) {
        this.buildingId = buildingId;
        this.forecast = forecast != null ? new ArrayList<>(forecast) : new ArrayList<>();
        this.recommendation = recommendation;
    }
    
    public List<ForecastPoint> getForecast() {
        return forecast != null ? new ArrayList<>(forecast) : new ArrayList<>();
    }
    
    public void setForecast(List<ForecastPoint> forecast) {
        this.forecast = forecast != null ? new ArrayList<>(forecast) : new ArrayList<>();
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastPoint {
        private String timestamp;
        private Double value;
    }
}
