package com.fujitsu.deliveryfeecalculator.controller;

import com.fujitsu.deliveryfeecalculator.model.entity.WeatherData;
import com.fujitsu.deliveryfeecalculator.repository.WeatherDataRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for accessing weather data.
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Tag(name = "Weather Data", description = "API to access weather data stored in the system")
public class WeatherDataController {

    private final WeatherDataRepository weatherDataRepository;

    /**
     * Get all weather data records from the database.
     *
     * @return List of all weather data records
     */
    @GetMapping
    @Operation(
            summary = "Get all weather data",
            description = "Retrieves all weather data records stored in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved weather data",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = WeatherData.class))
            )
    )
    public List<WeatherData> getAllWeatherData() {
        return weatherDataRepository.findAll();
    }
}