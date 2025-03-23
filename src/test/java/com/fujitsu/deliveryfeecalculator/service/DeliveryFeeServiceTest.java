package com.fujitsu.deliveryfeecalculator.service;

import com.fujitsu.deliveryfeecalculator.exception.DeliveryFeeCalculationException;
import com.fujitsu.deliveryfeecalculator.exception.WeatherDataNotFoundException;
import com.fujitsu.deliveryfeecalculator.model.entity.WeatherData;
import com.fujitsu.deliveryfeecalculator.model.enums.City;
import com.fujitsu.deliveryfeecalculator.model.enums.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeliveryFeeServiceTest {

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private DeliveryFeeServiceImpl deliveryFeeService;

    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.of(2024, 3, 15, 12, 0);
    }

    @Nested
    @DisplayName("Regional Base Fee Tests")
    class RegionalBaseFeeTests {

        private WeatherData normalWeather;

        @BeforeEach
        void setUp() {
            // Create normal weather with no extra fees
            normalWeather = WeatherData.builder()
                    .stationName("Test Station")
                    .airTemperature(10.0)
                    .windSpeed(5.0)
                    .weatherPhenomenon("clear")
                    .timestamp(testTime)
                    .build();
        }

        @ParameterizedTest
        @CsvSource({
                "TALLINN, CAR, 4.00",
                "TALLINN, SCOOTER, 3.50",
                "TALLINN, BIKE, 3.00",
                "TARTU, CAR, 3.50",
                "TARTU, SCOOTER, 3.00",
                "TARTU, BIKE, 2.50",
                "PARNU, CAR, 3.00",
                "PARNU, SCOOTER, 2.50",
                "PARNU, BIKE, 2.00"
        })
        @DisplayName("Should calculate correct regional base fee for all city and vehicle combinations")
        void shouldCalculateCorrectRegionalBaseFee(City city, VehicleType vehicleType, String expectedFee) {
            // Arrange
            normalWeather.setStationName(city.getStationName());
            when(weatherService.getLatestWeatherData(city)).thenReturn(normalWeather);

            // Act
            BigDecimal fee = deliveryFeeService.calculateFee(city, vehicleType);

            // Assert
            assertEquals(new BigDecimal(expectedFee), fee);
        }
    }

    @Nested
    @DisplayName("Temperature Fee Tests")
    class TemperatureFeeTests {

        @ParameterizedTest
        @CsvSource({
                "SCOOTER, -15.0, 1.00", // Very cold, extra fee 1.00€
                "BIKE, -15.0, 1.00",    // Very cold, extra fee 1.00€
                "SCOOTER, -5.0, 0.50",  // Cold, extra fee 0.50€
                "BIKE, -5.0, 0.50",     // Cold, extra fee 0.50€
                "SCOOTER, 5.0, 0.00",   // Normal, no extra fee
                "BIKE, 5.0, 0.00",      // Normal, no extra fee
                "CAR, -15.0, 0.00"      // Car should have no extra fee regardless of temperature
        })
        @DisplayName("Should calculate correct temperature fee")
        void shouldCalculateCorrectTemperatureFee(VehicleType vehicleType, double temperature, String expectedExtraFee) {
            // Arrange
            City city = City.TALLINN;
            WeatherData weatherData = WeatherData.builder()
                    .stationName(city.getStationName())
                    .airTemperature(temperature)
                    .windSpeed(5.0) // Normal wind
                    .weatherPhenomenon("clear") // Normal weather
                    .timestamp(testTime)
                    .build();

            when(weatherService.getLatestWeatherData(city)).thenReturn(weatherData);

            // Act
            BigDecimal totalFee = deliveryFeeService.calculateFee(city, vehicleType);

            // Calculate expected fee (base fee + expected extra fee)
            BigDecimal baseFee = getBaseFee(city, vehicleType);
            BigDecimal expectedTotalFee = baseFee.add(new BigDecimal(expectedExtraFee));

            // Assert
            assertEquals(expectedTotalFee, totalFee);
        }
    }

    @Nested
    @DisplayName("Wind Speed Fee Tests")
    class WindSpeedFeeTests {

        @Test
        @DisplayName("Should add wind fee for bike with moderate wind")
        void shouldAddWindFeeForBikeWithModerateWind() {
            // Arrange
            City city = City.TALLINN;
            WeatherData weatherData = WeatherData.builder()
                    .stationName(city.getStationName())
                    .airTemperature(10.0) // Normal temperature
                    .windSpeed(15.0) // Moderate wind (between 10-20 m/s)
                    .weatherPhenomenon("clear") // Normal weather
                    .timestamp(testTime)
                    .build();

            when(weatherService.getLatestWeatherData(city)).thenReturn(weatherData);

            // Act
            BigDecimal fee = deliveryFeeService.calculateFee(city, VehicleType.BIKE);

            // Assert - Base fee (3.00) + Wind fee (0.50) = 3.50
            assertEquals(new BigDecimal("3.50"), fee);
        }

        @Test
        @DisplayName("Should not add wind fee for scooter regardless of wind speed")
        void shouldNotAddWindFeeForScooter() {
            // Arrange
            City city = City.TALLINN;
            WeatherData weatherData = WeatherData.builder()
                    .stationName(city.getStationName())
                    .airTemperature(10.0) // Normal temperature
                    .windSpeed(15.0) // Moderate wind (between 10-20 m/s)
                    .weatherPhenomenon("clear") // Normal weather
                    .timestamp(testTime)
                    .build();

            when(weatherService.getLatestWeatherData(city)).thenReturn(weatherData);

            // Act
            BigDecimal fee = deliveryFeeService.calculateFee(city, VehicleType.SCOOTER);

            // Assert - Only base fee (3.50), no wind fee
            assertEquals(new BigDecimal("3.50"), fee);
        }

        @Test
        @DisplayName("Should throw exception for bike in high wind")
        void shouldThrowExceptionForBikeInHighWind() {
            // Arrange
            City city = City.TALLINN;
            WeatherData weatherData = WeatherData.builder()
                    .stationName(city.getStationName())
                    .airTemperature(10.0) // Normal temperature
                    .windSpeed(25.0) // High wind (>20 m/s)
                    .weatherPhenomenon("clear") // Normal weather
                    .timestamp(testTime)
                    .build();

            when(weatherService.getLatestWeatherData(city)).thenReturn(weatherData);

            // Act & Assert
            DeliveryFeeCalculationException exception = assertThrows(
                    DeliveryFeeCalculationException.class,
                    () -> deliveryFeeService.calculateFee(city, VehicleType.BIKE)
            );

            assertTrue(exception.getMessage().contains("forbidden"));
        }
    }

    @Nested
    @DisplayName("Weather Phenomenon Fee Tests")
    class WeatherPhenomenonFeeTests {

        @ParameterizedTest
        @CsvSource({
                "SCOOTER, light snowfall, 1.00",
                "BIKE, heavy snowfall, 1.00",
                "SCOOTER, light sleet, 1.00",
                "BIKE, moderate sleet, 1.00",
                "SCOOTER, light rain, 0.50",
                "BIKE, heavy rain, 0.50",
                "SCOOTER, clear, 0.00",
                "BIKE, cloudy with clear spells, 0.00",
                "CAR, heavy snowfall, 0.00"
        })
        @DisplayName("Should calculate correct weather phenomenon fee")
        void shouldCalculateCorrectWeatherPhenomenonFee(VehicleType vehicleType, String phenomenon, String expectedExtraFee) {
            // Arrange
            City city = City.TALLINN;
            WeatherData weatherData = WeatherData.builder()
                    .stationName(city.getStationName())
                    .airTemperature(10.0) // Normal temperature
                    .windSpeed(5.0) // Normal wind
                    .weatherPhenomenon(phenomenon)
                    .timestamp(testTime)
                    .build();

            when(weatherService.getLatestWeatherData(city)).thenReturn(weatherData);

            // Act
            BigDecimal totalFee = deliveryFeeService.calculateFee(city, vehicleType);

            // Calculate expected fee (base fee + expected extra fee)
            BigDecimal baseFee = getBaseFee(city, vehicleType);
            BigDecimal expectedTotalFee = baseFee.add(new BigDecimal(expectedExtraFee));

            // Assert
            assertEquals(expectedTotalFee, totalFee);
        }

        @ParameterizedTest
        @CsvSource({
                "SCOOTER, glaze",
                "BIKE, hail",
                "SCOOTER, thunder",
                "BIKE, thunderstorm"
        })
        @DisplayName("Should throw exception for forbidden weather phenomena")
        void shouldThrowExceptionForForbiddenWeatherPhenomena(VehicleType vehicleType, String phenomenon) {
            // Arrange
            City city = City.TALLINN;
            WeatherData weatherData = WeatherData.builder()
                    .stationName(city.getStationName())
                    .airTemperature(10.0) // Normal temperature
                    .windSpeed(5.0) // Normal wind
                    .weatherPhenomenon(phenomenon)
                    .timestamp(testTime)
                    .build();

            when(weatherService.getLatestWeatherData(city)).thenReturn(weatherData);

            // Act & Assert
            DeliveryFeeCalculationException exception = assertThrows(
                    DeliveryFeeCalculationException.class,
                    () -> deliveryFeeService.calculateFee(city, vehicleType)
            );

            assertTrue(exception.getMessage().contains("forbidden"));
        }
    }

    @Nested
    @DisplayName("Combined Condition Fee Tests")
    class CombinedConditionFeeTests {

        @Test
        @DisplayName("Should correctly calculate fee with multiple extra fees")
        void shouldCalculateFeeWithMultipleExtraFees() {
            // Arrange
            City city = City.TALLINN;
            WeatherData weatherData = WeatherData.builder()
                    .stationName(city.getStationName())
                    .airTemperature(-5.0) // Cold: +0.50€
                    .windSpeed(15.0) // Moderate wind: +0.50€ (for BIKE only)
                    .weatherPhenomenon("light snowfall") // Snow: +1.00€
                    .timestamp(testTime)
                    .build();

            when(weatherService.getLatestWeatherData(city)).thenReturn(weatherData);

            // Act
            BigDecimal scooterFee = deliveryFeeService.calculateFee(city, VehicleType.SCOOTER);
            BigDecimal bikeFee = deliveryFeeService.calculateFee(city, VehicleType.BIKE);

            // Assert
            // Scooter: Base fee (3.50) + Cold fee (0.50) + Snow fee (1.00) = 5.00
            assertEquals(new BigDecimal("5.00"), scooterFee);

            // Bike: Base fee (3.00) + Cold fee (0.50) + Wind fee (0.50) + Snow fee (1.00) = 5.00
            assertEquals(new BigDecimal("5.00"), bikeFee);
        }

        @Test
        @DisplayName("Should always return only the base fee for cars regardless of weather")
        void shouldReturnOnlyBaseFeeForCars() {
            // Arrange
            City city = City.TALLINN;
            WeatherData extremeWeather = WeatherData.builder()
                    .stationName(city.getStationName())
                    .airTemperature(-20.0) // Very cold
                    .windSpeed(19.0) // High wind
                    .weatherPhenomenon("heavy snowfall") // Snow
                    .timestamp(testTime)
                    .build();

            when(weatherService.getLatestWeatherData(city)).thenReturn(extremeWeather);

            // Act
            BigDecimal fee = deliveryFeeService.calculateFee(city, VehicleType.CAR);

            // Assert - Only base fee (4.00) for car
            assertEquals(new BigDecimal("4.00"), fee);
        }
    }

    @Nested
    @DisplayName("Historical Fee Calculation Tests")
    class HistoricalFeeCalculationTests {

        @Test
        @DisplayName("Should calculate historical fee based on past weather data")
        void shouldCalculateHistoricalFee() {
            // Arrange
            City city = City.TALLINN;
            VehicleType vehicleType = VehicleType.SCOOTER;
            LocalDateTime pastTime = LocalDateTime.of(2024, 1, 15, 12, 0);

            WeatherData pastWeather = WeatherData.builder()
                    .stationName(city.getStationName())
                    .airTemperature(-15.0) // Very cold: +1.00€
                    .windSpeed(5.0) // Normal wind
                    .weatherPhenomenon("heavy snowfall") // Snow: +1.00€
                    .timestamp(pastTime)
                    .build();

            when(weatherService.getWeatherDataByTimestamp(city, pastTime)).thenReturn(pastWeather);

            // Act
            BigDecimal fee = deliveryFeeService.calculateFee(city, vehicleType, pastTime);

            // Assert - Base fee (3.50) + Cold fee (1.00) + Snow fee (1.00) = 5.50
            assertEquals(new BigDecimal("5.50"), fee);
        }

        @Test
        @DisplayName("Should properly propagate weather data not found exception")
        void shouldPropagateWeatherDataNotFoundException() {
            // Arrange
            City city = City.TALLINN;
            VehicleType vehicleType = VehicleType.SCOOTER;
            LocalDateTime pastTime = LocalDateTime.of(2023, 1, 1, 12, 0);

            when(weatherService.getWeatherDataByTimestamp(city, pastTime))
                    .thenThrow(new WeatherDataNotFoundException("No historical data available"));

            // Act & Assert
            WeatherDataNotFoundException exception = assertThrows(
                    WeatherDataNotFoundException.class,
                    () -> deliveryFeeService.calculateFee(city, vehicleType, pastTime)
            );

            assertEquals("No historical data available", exception.getMessage());
        }
    }

    /**
     * Helper method to get the base fee for a city and vehicle type combination.
     */
    private BigDecimal getBaseFee(City city, VehicleType vehicleType) {
        return switch (city) {
            case TALLINN -> switch (vehicleType) {
                case CAR -> new BigDecimal("4.00");
                case SCOOTER -> new BigDecimal("3.50");
                case BIKE -> new BigDecimal("3.00");
            };
            case TARTU -> switch (vehicleType) {
                case CAR -> new BigDecimal("3.50");
                case SCOOTER -> new BigDecimal("3.00");
                case BIKE -> new BigDecimal("2.50");
            };
            case PARNU -> switch (vehicleType) {
                case CAR -> new BigDecimal("3.00");
                case SCOOTER -> new BigDecimal("2.50");
                case BIKE -> new BigDecimal("2.00");
            };
        };
    }
}