package com.fujitsu.deliveryfeecalculator.repository;

import com.fujitsu.deliveryfeecalculator.model.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    /**
     * Find the latest weather data for a specific station.
     */
    @Query("SELECT w FROM WeatherData w WHERE w.stationName = :stationName ORDER BY w.timestamp DESC LIMIT 1")
    Optional<WeatherData> findLatestByStationName(@Param("stationName") String stationName);

    /**
     * Find the weather data for a specific station closest to the provided timestamp.
     */
    @Query("SELECT w FROM WeatherData w WHERE w.stationName = :stationName AND w.timestamp <= :timestamp ORDER BY w.timestamp DESC LIMIT 1")
    Optional<WeatherData> findClosestByStationNameAndTimestamp(
            @Param("stationName") String stationName,
            @Param("timestamp") LocalDateTime timestamp);
}
