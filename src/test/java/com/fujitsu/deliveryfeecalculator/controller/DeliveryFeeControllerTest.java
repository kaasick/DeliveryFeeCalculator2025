package com.fujitsu.deliveryfeecalculator.controller;

import com.fujitsu.deliveryfeecalculator.dto.DeliveryFeeResponse;
import com.fujitsu.deliveryfeecalculator.exception.DeliveryFeeCalculationException;
import com.fujitsu.deliveryfeecalculator.exception.WeatherDataNotFoundException;
import com.fujitsu.deliveryfeecalculator.model.enums.City;
import com.fujitsu.deliveryfeecalculator.model.enums.VehicleType;
import com.fujitsu.deliveryfeecalculator.service.DeliveryFeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryFeeControllerTest {

    @Mock
    private DeliveryFeeService deliveryFeeService;

    @InjectMocks
    private DeliveryFeeController deliveryFeeController;

    @Test
    @DisplayName("Should calculate delivery fee for valid request")
    void calculateDeliveryFee_validRequest_returnsCorrectFee() {
        // Arrange
        BigDecimal expectedFee = new BigDecimal("4.00");
        when(deliveryFeeService.calculateFee(City.TALLINN, VehicleType.CAR))
                .thenReturn(expectedFee);

        // Act
        ResponseEntity<DeliveryFeeResponse> response = deliveryFeeController
                .calculateDeliveryFee("TALLINN", "CAR");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedFee, response.getBody().getFee());
        assertNull(response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return bad request when vehicle is forbidden")
    void calculateDeliveryFee_forbiddenVehicle_returnsBadRequest() {
        // Arrange
        String errorMessage = "Usage of selected vehicle type is forbidden";
        when(deliveryFeeService.calculateFee(City.TALLINN, VehicleType.BIKE))
                .thenThrow(new DeliveryFeeCalculationException(errorMessage));

        // Act
        ResponseEntity<DeliveryFeeResponse> response = deliveryFeeController
                .calculateDeliveryFee("TALLINN", "BIKE");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getFee());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return bad request for invalid city")
    void calculateDeliveryFee_invalidCity_returnsBadRequest() {
        // Act
        ResponseEntity<DeliveryFeeResponse> response = deliveryFeeController
                .calculateDeliveryFee("INVALID_CITY", "CAR");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid"));
    }

    @Test
    @DisplayName("Should return bad request for invalid vehicle type")
    void calculateDeliveryFee_invalidVehicleType_returnsBadRequest() {
        // Act
        ResponseEntity<DeliveryFeeResponse> response = deliveryFeeController
                .calculateDeliveryFee("TALLINN", "INVALID_VEHICLE");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid"));
    }

    @Test
    @DisplayName("Should return server error for unexpected exceptions")
    void calculateDeliveryFee_unexpectedException_returnsServerError() {
        // Arrange
        when(deliveryFeeService.calculateFee(City.TALLINN, VehicleType.CAR))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<DeliveryFeeResponse> response = deliveryFeeController
                .calculateDeliveryFee("TALLINN", "CAR");

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("unexpected error"));
    }

    @Test
    @DisplayName("Should calculate historical delivery fee for valid request")
    void calculateDeliveryFeeAtTime_validRequest_returnsCorrectFee() {
        // Arrange
        LocalDateTime testTime = LocalDateTime.of(2024, 3, 1, 12, 0);
        BigDecimal expectedFee = new BigDecimal("3.50");

        when(deliveryFeeService.calculateFee(City.TALLINN, VehicleType.CAR, testTime))
                .thenReturn(expectedFee);

        // Act
        ResponseEntity<DeliveryFeeResponse> response = deliveryFeeController
                .calculateDeliveryFeeAtTime("TALLINN", "CAR", testTime);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedFee, response.getBody().getFee());
        assertNull(response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return not found when weather data not available")
    void calculateDeliveryFeeAtTime_weatherDataNotFound_returnsNotFound() {
        // Arrange
        LocalDateTime testTime = LocalDateTime.of(2024, 3, 1, 12, 0);
        String errorMessage = "No weather data available";

        when(deliveryFeeService.calculateFee(City.TALLINN, VehicleType.CAR, testTime))
                .thenThrow(new WeatherDataNotFoundException(errorMessage));

        // Act
        ResponseEntity<DeliveryFeeResponse> response = deliveryFeeController
                .calculateDeliveryFeeAtTime("TALLINN", "CAR", testTime);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getFee());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return bad request when vehicle is forbidden for historical data")
    void calculateDeliveryFeeAtTime_forbiddenVehicle_returnsBadRequest() {
        // Arrange
        LocalDateTime testTime = LocalDateTime.of(2024, 3, 1, 12, 0);
        String errorMessage = "Usage of selected vehicle type is forbidden";

        when(deliveryFeeService.calculateFee(City.TALLINN, VehicleType.BIKE, testTime))
                .thenThrow(new DeliveryFeeCalculationException(errorMessage));

        // Act
        ResponseEntity<DeliveryFeeResponse> response = deliveryFeeController
                .calculateDeliveryFeeAtTime("TALLINN", "BIKE", testTime);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getFee());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return bad request for invalid parameters in historical request")
    void calculateDeliveryFeeAtTime_invalidInput_returnsBadRequest() {
        // Arrange
        LocalDateTime testTime = LocalDateTime.of(2024, 3, 1, 12, 0);

        // Act
        ResponseEntity<DeliveryFeeResponse> response = deliveryFeeController
                .calculateDeliveryFeeAtTime("INVALID_CITY", "CAR", testTime);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getFee());
        assertEquals("Invalid parameters provided", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return server error for unexpected exceptions in historical request")
    void calculateDeliveryFeeAtTime_unexpectedException_returnsServerError() {
        // Arrange
        LocalDateTime testTime = LocalDateTime.of(2024, 3, 1, 12, 0);

        when(deliveryFeeService.calculateFee(City.TALLINN, VehicleType.CAR, testTime))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<DeliveryFeeResponse> response = deliveryFeeController
                .calculateDeliveryFeeAtTime("TALLINN", "CAR", testTime);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("unexpected error"));
    }
}