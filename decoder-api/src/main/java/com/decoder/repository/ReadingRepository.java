package com.decoder.repository;

import com.decoder.model.Reading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReadingRepository extends JpaRepository<Reading, Long> {
    
    List<Reading> findByBuildingIdAndTimestampAfterOrderByTimestampDesc(
            Long buildingId, LocalDateTime timestamp);
    
    @Query("SELECT r FROM Reading r WHERE r.buildingId = :buildingId " +
           "AND r.timestamp >= :fromTimestamp ORDER BY r.timestamp DESC")
    List<Reading> findLastReadingsByBuildingId(
            @Param("buildingId") Long buildingId,
            @Param("fromTimestamp") LocalDateTime fromTimestamp);
    
    List<Reading> findByBuildingIdOrderByTimestampDesc(Long buildingId);
}
