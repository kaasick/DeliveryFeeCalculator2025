package com.fujitsu.deliveryfeecalculator.service;

import com.fujitsu.deliveryfeecalculator.model.entity.WeatherData;
import com.fujitsu.deliveryfeecalculator.model.enums.City;

import java.time.LocalDateTime;

public interface WeatherService {
    /**
     * Get the latest weather data for a specific city.
     */
    WeatherData getLatestWeatherData(City city);

    void fetchAndStoreWeatherData();
}
