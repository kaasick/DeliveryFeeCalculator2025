package com.fujitsu.deliveryfeecalculator.controller;

import com.fujitsu.deliveryfeecalculator.model.entity.WeatherData;
import com.fujitsu.deliveryfeecalculator.repository.WeatherDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeliveryFeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WeatherDataRepository weatherDataRepository;

    @Test
    @DisplayName("Application context loads")
    void applicationContextLoads() {
        // This test will fail if the application context cannot be loaded
    }

    @Test
    @DisplayName("Controller responds to basic request")
    void controllerResponds() throws Exception {
        // Setup test data
        setupCurrentWeatherData();

        // Test that the endpoint responds with 200 OK
        mockMvc.perform(get("/api/delivery-fee/TALLINN/CAR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Calculate fee for Tartu with Scooter in cold weather")
    void calculateFeeForTartuWithScooter() throws Exception {
        // Setup test data
        setupCurrentWeatherData();

        mockMvc.perform(get("/api/delivery-fee/TARTU/SCOOTER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andDo(print()); // Print response for debugging
    }

    @Test
    @DisplayName("Return error for Bike in extreme wind")
    void errorForBikeInExtremeWind() throws Exception {
        // Setup test data with extreme wind conditions
        WeatherData extremeWindData = WeatherData.builder()
                .stationName("Pärnu")
                .airTemperature(10.0)
                .windSpeed(25.0) // Extreme wind over 20.0 m/s
                .weatherPhenomenon("clear")
                .timestamp(LocalDateTime.now())
                .build();

        weatherDataRepository.save(extremeWindData);

        // Verify the data exists
        Optional<WeatherData> saved = weatherDataRepository.findLatestByStationName("Pärnu");
        assertThat(saved).isPresent();
        saved.ifPresent(data -> assertThat(data.getWindSpeed()).isGreaterThan(20.0));

        // Make the API call
        mockMvc.perform(get("/api/delivery-fee/PARNU/BIKE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.fee").doesNotExist())
                .andExpect(content().string(containsString("forbidden")))
                .andDo(print()); // Print response for debugging
    }

    @Test
    @DisplayName("Return error for invalid city")
    void errorForInvalidCity() throws Exception {
        mockMvc.perform(get("/api/delivery-fee/LONDON/CAR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("Invalid")))
                .andDo(print()); // Print response for debugging
    }

    @Test
    @DisplayName("Return error for invalid vehicle type")
    void errorForInvalidVehicleType() throws Exception {
        mockMvc.perform(get("/api/delivery-fee/TALLINN/HELICOPTER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("Invalid")))
                .andDo(print()); // Print response for debugging
    }

    @Test
    @DisplayName("Test historical fee calculation")
    void testHistoricalFeeCalculation() throws Exception {
        // Create historical weather record
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastTime = now.minusHours(2);

        // Always create new historical data to ensure the test passes
        WeatherData pastWeather = WeatherData.builder()
                .stationName("Tallinn-Harku")
                .airTemperature(-5.0) // Cold: +0.50€
                .windSpeed(5.0) // Normal wind
                .weatherPhenomenon("light snow") // Snow: +1.00€
                .timestamp(pastTime)
                .build();

        weatherDataRepository.save(pastWeather);

        mockMvc.perform(get("/api/delivery-fee/TALLINN/BIKE/at")
                        .param("datetime", pastTime.format(DateTimeFormatter.ISO_DATE_TIME))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andDo(print()); // Print response for debugging
    }

    @Test
    @DisplayName("Test historical data not found")
    void testHistoricalDataNotFound() throws Exception {
        LocalDateTime veryOldTime = LocalDateTime.now().minusYears(10);

        mockMvc.perform(get("/api/delivery-fee/TALLINN/BIKE/at")
                        .param("datetime", veryOldTime.format(DateTimeFormatter.ISO_DATE_TIME))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("No weather data available")))
                .andDo(print()); // Print response for debugging
    }

    // Helper method to setup current weather data
    private void setupCurrentWeatherData() {
        LocalDateTime now = LocalDateTime.now();

        // Tallinn - normal conditions
        createWeatherData("Tallinn-Harku", 10.0, 5.0, "clear", now);

        // Tartu - chilly weather
        createWeatherData("Tartu-Tõravere", -5.0, 5.0, "clear", now);

        // Pärnu - extreme wind
        createWeatherData("Pärnu", 10.0, 25.0, "clear", now);
    }

    // Helper method to create weather data only if it doesn't exist
    private void createWeatherData(String stationName, double airTemp, double windSpeed, String phenomenon, LocalDateTime timestamp) {
        // Try to find matching data first
        Optional<WeatherData> existingData = weatherDataRepository.findAll().stream()
                .filter(w -> w.getStationName().equals(stationName) &&
                        Math.abs(w.getTimestamp().getMinute() - timestamp.getMinute()) < 10)
                .findFirst();

        // Only create if we don't have similar data
        if (existingData.isEmpty()) {
            WeatherData weatherData = WeatherData.builder()
                    .stationName(stationName)
                    .airTemperature(airTemp)
                    .windSpeed(windSpeed)
                    .weatherPhenomenon(phenomenon)
                    .timestamp(timestamp)
                    .build();

            weatherDataRepository.save(weatherData);
        }
    }
}