package com.fujitsu.deliveryfeecalculator.service;

import com.fujitsu.deliveryfeecalculator.exception.WeatherDataNotFoundException;
import com.fujitsu.deliveryfeecalculator.model.entity.WeatherData;
import com.fujitsu.deliveryfeecalculator.model.enums.City;
import com.fujitsu.deliveryfeecalculator.model.weather.WeatherResponse;
import com.fujitsu.deliveryfeecalculator.model.weather.WeatherStation;
import com.fujitsu.deliveryfeecalculator.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WeatherDataRepository weatherDataRepository;
    private final RestTemplate restTemplate;

    @Value("${weather.api.url}")
    private String weatherApiUrl;

    // Constants to replace magic strings and numbers
    private static final String DEFAULT_STRING_VALUE = "Unknown";
    private static final double DEFAULT_NUMERIC_VALUE = 0.0;

    // Monitored stations (consider making this configurable for extensibility)
    private static final Set<String> MONITORED_STATIONS = Arrays.stream(City.values())
            .map(City::getStationName)
            .collect(Collectors.toSet());

    @Override
    public WeatherData getLatestWeatherData(City city) {
        return weatherDataRepository.findLatestByStationName(city.getStationName())
                .orElseThrow(() -> new WeatherDataNotFoundException(
                        "No weather data available for station: " + city.getStationName()));
    }

    @Override
    @Scheduled(cron = "${weather.fetch.cron}")
    public void fetchAndStoreWeatherData() {
        log.info("Fetching weather data from external service at {}", LocalDateTime.now());
        try {
            WeatherResponse response = restTemplate.getForObject(weatherApiUrl, WeatherResponse.class);

            if (response == null || response.getStations() == null || response.getStations().isEmpty()) {
                log.warn("Received empty or null response from weather service");
                return;
            }

            List<WeatherData> weatherDataList = response.getStations().stream()
                    .filter(station -> MONITORED_STATIONS.contains(station.getName()))
                    .map(this::convertToWeatherData)
                    .collect(Collectors.toList());

            if (weatherDataList.isEmpty()) {
                log.warn("No monitored stations found in the weather service response");
                return;
            }

            weatherDataRepository.saveAll(weatherDataList);
            log.info("Successfully stored {} weather data records", weatherDataList.size());
        } catch (RestClientException e) {
            log.error("Failed to fetch weather data from API: {}", e.getMessage(), e);
            // Consider adding retry logic here
        } catch (IllegalArgumentException e) {
            log.error("Invalid data format: {}", e.getMessage(), e);
            // Handle parsing errors
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            // Fallback for truly unexpected issues
        }
    }

    private WeatherData convertToWeatherData(WeatherStation station) {
        if (station == null) {
            throw new IllegalArgumentException("WeatherStation cannot be null");
        }
        LocalDateTime fetchTime = LocalDateTime.now();
        return WeatherData.builder()
                .stationName(station.getName() != null ? station.getName() : DEFAULT_STRING_VALUE)
                .wmoCode(station.getWmoCode() != null ? station.getWmoCode() : DEFAULT_STRING_VALUE)
                .airTemperature(station.getAirTemperature() != null ? station.getAirTemperature() : DEFAULT_NUMERIC_VALUE)
                .windSpeed(station.getWindSpeed() != null ? station.getWindSpeed() : DEFAULT_NUMERIC_VALUE)
                .weatherPhenomenon(station.getPhenomenon() != null ? station.getPhenomenon() : DEFAULT_STRING_VALUE)
                .timestamp(fetchTime)
                .build();
    }
}