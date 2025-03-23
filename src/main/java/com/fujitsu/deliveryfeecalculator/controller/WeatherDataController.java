package com.fujitsu.deliveryfeecalculator.controller;

import com.fujitsu.deliveryfeecalculator.model.entity.WeatherData;
import com.fujitsu.deliveryfeecalculator.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherDataController {

    private final WeatherDataRepository weatherDataRepository;

    @GetMapping
    public List<WeatherData> getAllWeatherData() {
        return weatherDataRepository.findAll();
    }
}
