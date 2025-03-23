package com.fujitsu.deliveryfeecalculator.controller;

import com.fujitsu.deliveryfeecalculator.exception.DeliveryFeeCalculationException;
import com.fujitsu.deliveryfeecalculator.dto.DeliveryFeeResponse;
import com.fujitsu.deliveryfeecalculator.exception.WeatherDataNotFoundException;
import com.fujitsu.deliveryfeecalculator.model.enums.City;
import com.fujitsu.deliveryfeecalculator.model.enums.VehicleType;
import com.fujitsu.deliveryfeecalculator.service.DeliveryFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * REST controller for delivery fee calculation.
 */
@Slf4j
@RestController
@RequestMapping("/api/delivery-fee")
@RequiredArgsConstructor
@Tag(name = "Delivery Fee Calculator", description = "API to calculate delivery fees based on city, vehicle type, and weather conditions")
public class DeliveryFeeController {

    private final DeliveryFeeService deliveryFeeService;

    /**
     * Calculate delivery fee based on city and vehicle type.
     *
     * @param city        The city (TALLINN, TARTU, PARNU)
     * @param vehicleType The vehicle type (CAR, SCOOTER, BIKE)
     * @return Delivery fee or error message
     */
    @GetMapping("/{city}/{vehicleType}")
    @Operation(
            summary = "Calculate delivery fee",
            description = "Calculates the delivery fee based on city, vehicle type, and current weather conditions"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful calculation",
                    content = @Content(schema = @Schema(implementation = DeliveryFeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or vehicle type forbidden due to weather conditions",
                    content = @Content(schema = @Schema(implementation = DeliveryFeeResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeliveryFeeResponse.class)))
    })
    public ResponseEntity<DeliveryFeeResponse> calculateDeliveryFee(
            @Parameter(description = "City name: TALLINN, TARTU, or PARNU", required = true)
            @PathVariable String city,
            @Parameter(description = "Vehicle type: CAR, SCOOTER, or BIKE", required = true)
            @PathVariable String vehicleType) {

        try {
            City cityEnum = City.valueOf(city.toUpperCase());
            VehicleType vehicleTypeEnum = VehicleType.valueOf(vehicleType.toUpperCase());

            BigDecimal fee = deliveryFeeService.calculateFee(cityEnum, vehicleTypeEnum);

            return ResponseEntity.ok(new DeliveryFeeResponse(fee));
        } catch (DeliveryFeeCalculationException e) {
            log.warn("Delivery calculation restriction: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new DeliveryFeeResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new DeliveryFeeResponse("Invalid city or vehicle type provided"));
        } catch (Exception e) {
            log.error("Error calculating delivery fee", e);
            return ResponseEntity.internalServerError()
                    .body(new DeliveryFeeResponse("An unexpected error occurred"));
        }
    }

    /**
     * Calculate delivery fee based on city, vehicle type, and specific datetime.
     *
     * @param city        The city (TALLINN, TARTU, PARNU)
     * @param vehicleType The vehicle type (CAR, SCOOTER, BIKE)
     * @param datetime    The datetime for historical calculation
     * @return Delivery fee or error message
     */
    @GetMapping("/{city}/{vehicleType}/at")
    @Operation(
            summary = "Calculate historical delivery fee",
            description = "Calculates the delivery fee based on city, vehicle type, and weather conditions at the specified time"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful calculation",
                    content = @Content(schema = @Schema(implementation = DeliveryFeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or vehicle type forbidden due to weather conditions",
                    content = @Content(schema = @Schema(implementation = DeliveryFeeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Weather data not found for the specified time",
                    content = @Content(schema = @Schema(implementation = DeliveryFeeResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeliveryFeeResponse.class)))
    })
    public ResponseEntity<DeliveryFeeResponse> calculateDeliveryFeeAtTime(
            @Parameter(description = "City name: TALLINN, TARTU, or PARNU", required = true)
            @PathVariable String city,
            @Parameter(description = "Vehicle type: CAR, SCOOTER, or BIKE", required = true)
            @PathVariable String vehicleType,
            @Parameter(description = "Datetime for historical calculation (ISO format: yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime datetime) {

        try {
            City cityEnum = City.valueOf(city.toUpperCase());
            VehicleType vehicleTypeEnum = VehicleType.valueOf(vehicleType.toUpperCase());

            BigDecimal fee = deliveryFeeService.calculateFee(cityEnum, vehicleTypeEnum, datetime);

            return ResponseEntity.ok(new DeliveryFeeResponse(fee));
        } catch (DeliveryFeeCalculationException e) {
            log.warn("Delivery calculation restriction for historical data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new DeliveryFeeResponse(e.getMessage()));
        } catch (WeatherDataNotFoundException e) {
            log.warn("Weather data not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DeliveryFeeResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid params: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new DeliveryFeeResponse("Invalid parameters provided"));
        } catch (Exception e) {
            log.error("Error calculating historical delivery fee", e);
            return ResponseEntity.internalServerError()
                    .body(new DeliveryFeeResponse("An unexpected error occurred"));
        }
    }
}