package com.fujitsu.deliveryfeecalculator.service;

import com.fujitsu.deliveryfeecalculator.exception.DeliveryFeeCalculationException;
import com.fujitsu.deliveryfeecalculator.model.entity.WeatherData;
import com.fujitsu.deliveryfeecalculator.model.enums.City;
import com.fujitsu.deliveryfeecalculator.model.enums.VehicleType;
import com.fujitsu.deliveryfeecalculator.model.enums.WeatherPhenomenon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Implementation of the DeliveryFeeService interface that calculates
 * delivery fees based on regional base fees and weather conditions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryFeeServiceImpl implements DeliveryFeeService {

    private final WeatherService weatherService;

    // Temperature thresholds
    private static final double COLD_TEMP_THRESHOLD = -10.0;
    private static final double COOL_TEMP_THRESHOLD = 0.0;

    // Wind speed thresholds
    private static final double HIGH_WIND_THRESHOLD = 20.0;
    private static final double MODERATE_WIND_THRESHOLD = 10.0;

    // Fee amounts in EUR
    private static final BigDecimal COLD_TEMP_FEE = new BigDecimal("1.00");
    private static final BigDecimal COOL_TEMP_FEE = new BigDecimal("0.50");
    private static final BigDecimal HIGH_WIND_FEE = new BigDecimal("0.50");
    private static final BigDecimal SNOW_SLEET_FEE = new BigDecimal("1.00");
    private static final BigDecimal RAIN_FEE = new BigDecimal("0.50");

    // Regional Base Fee constants for each city and vehicle type
    private static final BigDecimal TALLINN_CAR_FEE = new BigDecimal("4.00");
    private static final BigDecimal TALLINN_SCOOTER_FEE = new BigDecimal("3.50");
    private static final BigDecimal TALLINN_BIKE_FEE = new BigDecimal("3.00");

    private static final BigDecimal TARTU_CAR_FEE = new BigDecimal("3.50");
    private static final BigDecimal TARTU_SCOOTER_FEE = new BigDecimal("3.00");
    private static final BigDecimal TARTU_BIKE_FEE = new BigDecimal("2.50");

    private static final BigDecimal PARNU_CAR_FEE = new BigDecimal("3.00");
    private static final BigDecimal PARNU_SCOOTER_FEE = new BigDecimal("2.50");
    private static final BigDecimal PARNU_BIKE_FEE = new BigDecimal("2.00");

    private static final BigDecimal ZERO_FEE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Override
    public BigDecimal calculateFee(City city, VehicleType vehicleType) {
        WeatherData weatherData = weatherService.getLatestWeatherData(city);
        return calculateFeeInternal(city, vehicleType, weatherData);
    }

    @Override
    public BigDecimal calculateFee(City city, VehicleType vehicleType, LocalDateTime timestamp) {
        //WeatherData weatherData = weatherService.getWeatherDataByTimestamp(city, timestamp);
        //return calculateFeeInternal(city, vehicleType, weatherData);
        return null;
    }

    /**
     * Internal method to calculate the fee based on provided weather data.
     *
     * @param city        the city
     * @param vehicleType the vehicle type
     * @param weatherData the weather data to use for calculation
     * @return the calculated fee
     */
    private BigDecimal calculateFeeInternal(City city, VehicleType vehicleType, WeatherData weatherData) {
        // Step 1: Calculate regional base fee (RBF)
        BigDecimal regionalBaseFee = calculateRegionalBaseFee(city, vehicleType);

        // Step 2: Calculate extra fees based on weather conditions
        BigDecimal extraTemperatureFee = calculateExtraTemperatureFee(vehicleType, weatherData.getAirTemperature());
        BigDecimal extraWindFee = calculateExtraWindFee(vehicleType, weatherData.getWindSpeed());
        BigDecimal extraWeatherPhenomenonFee = calculateExtraWeatherPhenomenonFee(
                vehicleType, weatherData.getWeatherPhenomenon());

        // Step 3: Calculate total fee (sum of all fees)
        BigDecimal totalFee = regionalBaseFee
                .add(extraTemperatureFee)
                .add(extraWindFee)
                .add(extraWeatherPhenomenonFee);

        log.info("Fee calculation for {} in {} with weather conditions [temp: {}, wind: {}, phenomenon: {}] = {}€",
                vehicleType, city, weatherData.getAirTemperature(), weatherData.getWindSpeed(),
                weatherData.getWeatherPhenomenon(), totalFee);

        return totalFee;
    }

    /**
     * Calculates the regional base fee based on city and vehicle type.
     */
    private BigDecimal calculateRegionalBaseFee(City city, VehicleType vehicleType) {
        return switch (city) {
            case TALLINN -> switch (vehicleType) {
                case CAR -> TALLINN_CAR_FEE;
                case SCOOTER -> TALLINN_SCOOTER_FEE;
                case BIKE -> TALLINN_BIKE_FEE;
            };
            case TARTU -> switch (vehicleType) {
                case CAR -> TARTU_CAR_FEE;
                case SCOOTER -> TARTU_SCOOTER_FEE;
                case BIKE -> TARTU_BIKE_FEE;
            };
            case PARNU -> switch (vehicleType) {
                case CAR -> PARNU_CAR_FEE;
                case SCOOTER -> PARNU_SCOOTER_FEE;
                case BIKE -> PARNU_BIKE_FEE;
            };
        };
    }

    /**
     * Calculates extra fee based on air temperature.
     * Only applies to SCOOTER and BIKE vehicle types.
     */
    private BigDecimal calculateExtraTemperatureFee(VehicleType vehicleType, double temperature) {
        if (vehicleType == VehicleType.CAR) {
            return ZERO_FEE;
        }

        if (temperature < COLD_TEMP_THRESHOLD) {
            return COLD_TEMP_FEE;
        } else if (temperature <= COOL_TEMP_THRESHOLD) {
            return COOL_TEMP_FEE;
        }

        return ZERO_FEE;
    }

    /**
     * Calculates extra fee based on wind speed.
     * Only applies to BIKE vehicle type.
     */
    private BigDecimal calculateExtraWindFee(VehicleType vehicleType, double windSpeed) {
        if (vehicleType != VehicleType.BIKE) {
            return ZERO_FEE;
        }

        if (windSpeed > HIGH_WIND_THRESHOLD) {
            throw new DeliveryFeeCalculationException("Usage of selected vehicle type is forbidden due to high wind speed");
        } else if (windSpeed >= MODERATE_WIND_THRESHOLD) {
            return HIGH_WIND_FEE;
        }

        return ZERO_FEE;
    }

    /**
     * Calculates extra fee based on weather phenomenon.
     * Only applies to SCOOTER and BIKE vehicle types.
     */
    private BigDecimal calculateExtraWeatherPhenomenonFee(VehicleType vehicleType, String phenomenon) {
        if (vehicleType == VehicleType.CAR) {
            return ZERO_FEE;
        }

        WeatherPhenomenon weatherCategory = WeatherPhenomenon.categorize(phenomenon);

        if (weatherCategory.isUsageForbidden()) {
            throw new DeliveryFeeCalculationException("Usage of selected vehicle type is forbidden due to dangerous weather conditions");
        }

        if (weatherCategory == WeatherPhenomenon.SNOW || weatherCategory == WeatherPhenomenon.SLEET) {
            return SNOW_SLEET_FEE;
        } else if (weatherCategory == WeatherPhenomenon.RAIN) {
            return RAIN_FEE;
        }

        return ZERO_FEE;
    }
}
