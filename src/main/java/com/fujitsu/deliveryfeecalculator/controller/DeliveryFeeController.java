package com.fujitsu.deliveryfeecalculator.controller;

import com.fujitsu.deliveryfeecalculator.exception.DeliveryFeeCalculationException;
import com.fujitsu.deliveryfeecalculator.dto.DeliveryFeeResponse;
import com.fujitsu.deliveryfeecalculator.model.enums.City;
import com.fujitsu.deliveryfeecalculator.model.enums.VehicleType;
import com.fujitsu.deliveryfeecalculator.service.DeliveryFeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
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
public class DeliveryFeeController {

    private final DeliveryFeeService deliveryFeeService;

    /**
     * Calculate delivery fee based on city and vehicle type.
     *
     * @param city The city (TALLINN, TARTU, PARNU)
     * @param vehicleType The vehicle type (CAR, SCOOTER, BIKE)
     * @return Delivery fee or error message
     */
    @GetMapping("/{city}/{vehicleType}")
    public ResponseEntity<DeliveryFeeResponse> calculateDeliveryFee(
            @PathVariable String city,
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
     * @param city The city (TALLINN, TARTU, PARNU)
     * @param vehicleType The vehicle type (CAR, SCOOTER, BIKE)
     * @param datetime The datetime for historical calculation
     * @return Delivery fee or error message
     */
    @GetMapping("/{city}/{vehicleType}/at")
    public ResponseEntity<DeliveryFeeResponse> calculateDeliveryFeeAtTime(
            @PathVariable String city,
            @PathVariable String vehicleType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime datetime) {

        try {
            City cityEnum = City.valueOf(city.toUpperCase());
            VehicleType vehicleTypeEnum = VehicleType.valueOf(vehicleType.toUpperCase());

            BigDecimal fee = deliveryFeeService.calculateFee(cityEnum, vehicleTypeEnum, datetime);

            return ResponseEntity.ok(new DeliveryFeeResponse(fee));
        } catch (DeliveryFeeCalculationException e) {
            log.warn("Delivery calculation restriction: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new DeliveryFeeResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new DeliveryFeeResponse("Invalid parameters provided"));
        } catch (Exception e) {
            log.error("Error calculating delivery fee", e);
            return ResponseEntity.internalServerError()
                    .body(new DeliveryFeeResponse("An unexpected error occurred"));
        }
    }
}